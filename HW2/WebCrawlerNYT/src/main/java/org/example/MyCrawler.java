package org.example;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class MyCrawler extends WebCrawler {
    // Regex to match file extensions to avoid.
    private final static Pattern FILTERS = Pattern.compile(".*(\\.(css|js|json|mp3|zip|gz))$");
    private final static String DOMAIN = "https://www.nytimes.com/";

    // Shared data structures to hold statistics from all threads.
    private final ConcurrentHashMap<String, String> fetchStats;
    private final ConcurrentHashMap<String, VisitStat> visitStats;
    private final ConcurrentHashMap<String, String> urlsStats;

    public MyCrawler(ConcurrentHashMap<String, String> fetchStats,
                     ConcurrentHashMap<String, VisitStat> visitStats,
                     ConcurrentHashMap<String, String> urlsStats) {
        this.fetchStats = fetchStats;
        this.visitStats = visitStats;
        this.urlsStats = urlsStats;
    }

    /**
     * This method is called for every page that is fetched.
     * We record its URL and the HTTP status code.
     */
    @Override
    protected void handlePageStatusCode(WebURL webUrl, int statusCode, String statusDescription) {
        String url = escapeCsv(webUrl.getURL());
        fetchStats.put(url, String.valueOf(statusCode));
    }

    /**
     * This method decides if a URL should be crawled or not.
     */
    @Override
    public boolean shouldVisit(Page referringPage, WebURL url) {
        String href = url.getURL().toLowerCase();
        String domain = url.getDomain().toLowerCase();

        // Record all discovered URLs, inside or outside the news site domain.
        if (domain.contains(DOMAIN)) {
            urlsStats.put(escapeCsv(href), "OK");
        } else {
            urlsStats.put(escapeCsv(href), "N_OK");
        }

        // Only visit URLs that are not filtered and are within the news site domain.
        return !FILTERS.matcher(href).matches() && domain.contains(DOMAIN);
    }

    /**
     * This method is called when a page is successfully downloaded and parsed.
     */
    @Override
    public void visit(Page page) {
        String url = escapeCsv(page.getWebURL().getURL());
        String contentType = page.getContentType().split(";")[0]; // Get content type without charset

        // We are interested in html, doc, pdf, and images.
        boolean isAllowedType = contentType.startsWith("text/html") ||
                contentType.startsWith("image/") ||
                contentType.contains("application/msword") || // .doc
                contentType.contains("application/vnd.openxmlformats-officedocument.wordprocessingml.document") || // .docx
                contentType.contains("application/pdf");

        if (isAllowedType) {
            int size = page.getContentData().length;
            int outlinks = 0;

            if (page.getParseData() instanceof HtmlParseData) {
                HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
                Set<WebURL> links = htmlParseData.getOutgoingUrls();
                outlinks = links.size();
            }

            visitStats.put(url, new VisitStat(size, outlinks, contentType));
        }
    }

    /**
     * A helper method to handle commas in data to prevent CSV format issues.
     * Replaces commas with a hyphen as suggested by the assignment.
     */
    private String escapeCsv(String data) {
        if (data == null) {
            return "";
        }
        // As per the assignment Q&A, replace commas to avoid breaking CSV format.
        return data.replace(",", "-");
    }

    /**
     * A static utility method to write generic statistics to a CSV file.
     */
    public static void writeStatsToCsv(String filename, Map<String, String> stats, String[] headers) {
        try (FileWriter writer = new FileWriter(filename)) {
            // Write header
            writer.append(String.join(",", headers)).append("\n");

            // Write data rows
            for (Map.Entry<String, String> entry : stats.entrySet()) {
                writer.append(entry.getKey()).append(",").append(entry.getValue()).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * A static utility method to write visit-specific statistics to a CSV file.
     */
    public static void writeVisitStatsToCsv(String filename, Map<String, VisitStat> stats, String[] headers) {
        try (FileWriter writer = new FileWriter(filename)) {
            // Write header
            writer.append(String.join(",", headers)).append("\n");

            // Write data rows
            for (Map.Entry<String, VisitStat> entry : stats.entrySet()) {
                VisitStat stat = entry.getValue();
                writer.append(entry.getKey()).append(",")
                        .append(String.valueOf(stat.getSize())).append(",")
                        .append(String.valueOf(stat.getOutlinks())).append(",")
                        .append(stat.getContentType()).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
