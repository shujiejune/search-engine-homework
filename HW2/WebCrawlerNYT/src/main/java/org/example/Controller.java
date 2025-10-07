package org.example;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class Controller {
    public static void main(String[] args) throws Exception {
        // The folder where fetched data will be stored.
        String crawlStorageFolder = "data/crawl";
        // Number of concurrent threads for crawling.
        int numberOfCrawlers = 7;

        CrawlConfig config = new CrawlConfig();
        config.setCrawlStorageFolder(crawlStorageFolder);
        // Set the maximum number of pages to fetch.
        config.setMaxPagesToFetch(10000);
        // Set the maximum depth of crawling.
        config.setMaxDepthOfCrawling(16);
        // Set a polite random delay between requests to avoid overloading the server.
        Random random = new Random();
        config.setPolitenessDelay(random.nextInt(201) + 100);
        // Crawl binary content like images and pdfs
        config.setIncludeBinaryContentInCrawling(true);

        // Instantiate the controller for this crawl.
        PageFetcher pageFetcher = new PageFetcher(config);
        RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
        robotstxtConfig.setEnabled(true);
        RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher);
        CrawlController controller = new CrawlController(config, pageFetcher, robotstxtServer);

        // Add the seed URL to start crawling.
        controller.addSeed("https://www.nytimes.com/");

        // Create shared data structures for statistics
        ConcurrentHashMap<String, String> fetchStats = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, VisitStat> visitStats = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, String> urlsStats = new ConcurrentHashMap<>();

        // Start the crawl. This is a blocking operation, meaning it will wait for the crawl to finish.
        CrawlController.WebCrawlerFactory<MyCrawler> factory = () -> new MyCrawler(fetchStats, visitStats, urlsStats);
        controller.start(factory, numberOfCrawlers);

        // After the crawl is finished, write the collected statistics to CSV files.
        System.out.println("Crawler is finished. Writing stats to CSV...");
        MyCrawler.writeStatsToCsv("fetch_nytimes.csv", fetchStats, new String[]{"URL", "Status"});
        MyCrawler.writeVisitStatsToCsv("visit_nytimes.csv", visitStats, new String[]{"URL", "Size (Bytes)", "# of Outlinks", "Content-Type"});
        MyCrawler.writeStatsToCsv("urls_nytimes.csv", urlsStats, new String[]{"URL", "Indicator"});
        System.out.println("CSV files have been created successfully.");
    }
}
