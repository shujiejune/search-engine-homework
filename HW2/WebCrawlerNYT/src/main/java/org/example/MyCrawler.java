package org.example;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;

import java.util.Set;
import java.util.regex.Pattern;

public class MyCrawler {
    // Regex to match file extensions we want to avoid.
    private final static Pattern FILTERS = Pattern.compile(".*(\\.(css|js|json|mp3|zip|gz))$");
    private final static String DOMAIN = "https://www.nytimes.com/";

    private CrawlStat myCrawlStat;

    public MyCrawler() {
        myCrawlStat = new CrawlStat();
    }

    /**
     * This method receives two parameters. The first parameter is the page
     * in which we have discovered this new url and the second parameter is
     * the new url. You should implement this function to specify whether
     * the given url should be crawled or not (based on your crawling logic).
     */
    @Override
    public boolean shouldVisit(Page referringPage, WebURL url) {
        String href = url.getURL().toLowerCase();

        // Add to URL stats
        if (href.startsWith(DOMAIN)) {
            myCrawlStat.addUrlStat(href, "OK");
        } else {
            myCrawlStat.addUrlStat(href, "N_OK");
        }

        // Only visit URLs within the domain and that do not match the filter.
        return !FILTERS.matcher(href).matches()
                && href.startsWith(DOMAIN);
    }

    /**
     * This function is called when a page is fetched and ready
     * to be processed by your program.
     */
    @Override
    public void visit(Page page) {
        String url = page.getWebURL().getURL();
        String contentType = page.getContentType().split(";")[0]; // Get content type without charset
        int size = page.getContentData().length;
        int outlinks = 0;

        // Only process allowed content types
        if (contentType.equals("text/html") || contentType.startsWith("image/") || contentType.equals("application/pdf") || contentType.equals("application/msword") || contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
            if (page.getParseData() instanceof HtmlParseData) {
                HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
                Set<WebURL> links = htmlParseData.getOutgoingUrls();
                outlinks = links.size();
            }
            myCrawlStat.addVisitStat(new VisitStat(url, size, outlinks, contentType));
        }
    }

    @Override
    protected void handlePageStatusCode(WebURL webUrl, int statusCode, String statusDescription) {
        myCrawlStat.addFetchStat(webUrl.getURL(), statusCode);
    }

    @Override
    public Object getMyLocalData() {
        return myCrawlStat;
    }
}
