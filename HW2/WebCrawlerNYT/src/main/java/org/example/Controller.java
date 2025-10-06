package org.example;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class Controller {
    public static void main(String[] args) throws Exception {
        // The folder where fetched data will be stored.
        String crawlStorageFolder = "data/crawl";
        // Number of concurrent threads for crawling.
        int numberOfCrawlers = 7;

        CrawlConfig config = new CrawlConfig();
        config.setCrawlStorageFolder(crawlStorageFolder);
        // Set the maximum number of pages to fetch.
        config.setMaxPagesToFetch(20000);
        // Set the maximum depth of crawling.
        config.setMaxDepthOfCrawling(16);
        // We need to crawl binary content like images and pdfs
        config.setIncludeBinaryContentInCrawling(true);
        // Set a polite delay between requests to avoid overloading the server.
        config.setPolitenessDelay(200);

        // Instantiate the controller for this crawl.
        PageFetcher pageFetcher = new PageFetcher(config);
        RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
        RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher);
        CrawlController controller = new CrawlController(config, pageFetcher, robotstxtServer);

        // Add the seed URL to start crawling.
        controller.addSeed("https://www.nytimes.com/");

        // Start the crawl. This is a blocking operation, meaning it will wait for the crawl to finish.
        controller.start(MyCrawler.class, numberOfCrawlers);

        // After the crawl is finished, collect and write the data to CSV files.
        List<Object> crawlersLocalData = controller.getCrawlersLocalData();

        // Initialize CSV writers
        FileWriter fetchCsv = new FileWriter("fetch_nytimes.csv");
        fetchCsv.append("URL,Status\n");

        FileWriter visitCsv = new FileWriter("visit_nytimes.csv");
        visitCsv.append("URL,Size (Bytes),# of Outlinks,Content-Type\n");

        FileWriter urlsCsv = new FileWriter("urls_nytimes.csv");
        urlsCsv.append("URL,Indicator\n");

        // Process data from each crawler thread
        for (Object localData : crawlersLocalData) {
            CrawlStat stats = (CrawlStat) localData;

            // Write to fetch_nytimes.csv
            for (Map.Entry<String, Integer> entry : stats.getFetchStats().entrySet()) {
                fetchCsv.append(escapeCsv(entry.getKey())).append(",").append(String.valueOf(entry.getValue())).append("\n");
            }

            // Write to visit_nytimes.csv
            for (VisitStat visitStat : stats.getVisitStats()) {
                visitCsv.append(escapeCsv(visitStat.getUrl())).append(",");
                visitCsv.append(String.valueOf(visitStat.getSize())).append(",");
                visitCsv.append(String.valueOf(visitStat.getOutlinks())).append(",");
                visitCsv.append(visitStat.getContentType()).append("\n");
            }

            // Write to urls_nytimes.csv
            for (Map.Entry<String, String> entry : stats.getUrlStats().entrySet()) {
                urlsCsv.append(escapeCsv(entry.getKey())).append(",").append(entry.getValue()).append("\n");
            }
        }

        // Close all file writers
        fetchCsv.flush();
        fetchCsv.close();
        visitCsv.flush();
        visitCsv.close();
        urlsCsv.flush();
        urlsCsv.close();

        System.out.println("Crawler finished. CSV files have been generated.");
    }

    // Helper function to escape commas in URLs for CSV format
    private static String escapeCsv(String data) {
        if (data.contains(",")) {
            return "\"" + data + "\"";
        }
        return data;
    }
}
