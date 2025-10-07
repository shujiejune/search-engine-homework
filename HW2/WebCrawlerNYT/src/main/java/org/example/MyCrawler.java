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

/**
 * WebCrawler.java class reference:
 * https://github.com/yasserg/crawler4j/blob/master/crawler4j/src/main/java/edu/uci/ics/crawler4j/crawler/WebCrawler.java
 */
public class MyCrawler extends WebCrawler {
    // Regex to match file extensions to avoid.
    private final static Pattern FILTERS = Pattern.compile(".*(\\.(css|js|json|mp3|zip|gz))$");
    private final String[] ALLOWED_DOMAINS = {"nytimes.com", "nyt.com"};

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
     * This method is called for every page that is fetched,
     * recording its URLs and the HTTP status codes.
     */
    @Override
    protected void handlePageStatusCode(WebURL webUrl, int statusCode, String statusDescription) {
        String url = escapeCsv(webUrl.getURL());
        fetchStats.put(url, String.valueOf(statusCode));
    }

    /**
     * This method is called for client and server errors (4xx, 5xx).
     * The signature MUST use String urlStr.
     */
    @Override
    protected void onUnexpectedStatusCode(String urlStr, int statusCode, String contentType, String description) {
        String url = escapeCsv(urlStr);
        fetchStats.put(url, String.valueOf(statusCode));
    }

    /**
     * This method is called when the crawler successfully fetches the headers (e.g., gets a 200 OK),
     * but fails to download the content/body of the page.
     */
    @Override
    protected void onContentFetchError(WebURL webUrl) {
        String url = escapeCsv(webUrl.getURL());
        // We use a custom code "0" to indicate this specific type of content fetch error.
        fetchStats.put(url, "0");
    }

    /**
     * This method decides if a URL should be crawled or not.
     * This method was completed with the help of Gemini.
     */
    @Override
    public boolean shouldVisit(Page referringPage, WebURL url) {
        String href = url.getURL().toLowerCase();
        String domain = url.getDomain().toLowerCase();

        boolean isAllowedDomain = false;
        for (String allowed : ALLOWED_DOMAINS) {
            if (domain.endsWith(allowed)) {
                isAllowedDomain = true;
                break;
            }
        }

        // Record all discovered URLs and their status (OK or N_OK)
        urlsStats.put(escapeCsv(href), isAllowedDomain ? "OK" : "N_OK");

        // Only visit URLs that are not filtered and are within the news site domain.
        return !FILTERS.matcher(href).matches() && isAllowedDomain;
    }

    /**
     * This method is called when a page is successfully downloaded and parsed.
     * This method was completed with the help of Gemini.
     */
    @Override
    public void visit(Page page) {
        String url = escapeCsv(page.getWebURL().getURL());
        String contentType = page.getContentType().split(";")[0]; // Get content type without charset

        // Only interested in html, doc, pdf, and images.
        boolean isAllowedType = contentType.startsWith("text/html") ||
                contentType.startsWith("image/") ||
                contentType.contains("application/msword") || // .doc
                contentType.contains("application/vnd.openxmlformats-officedocument.wordprocessingml.document") || // .docx
                contentType.contains("application/pdf");

        if (isAllowedType) {
            int size = page.getContentData().length;
            int outlinks = 0;

            // Check at the runtime, page.getParseData() returns a general class ParseData
            if (page.getParseData() instanceof HtmlParseData) {
                // Cast because the compiler will look for the getOutgoingUrls() method
                // but ParseData doesn't have it.
                HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
                Set<WebURL> links = htmlParseData.getOutgoingUrls();
                outlinks = links.size();
            }

            visitStats.put(url, new VisitStat(size, outlinks, contentType));
        }
    }

    /**
     * A helper method to handle commas in data to prevent CSV format issues.
     * Replaces commas with a hyphen.
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
     * This method was completed with the help of Gemini.
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
