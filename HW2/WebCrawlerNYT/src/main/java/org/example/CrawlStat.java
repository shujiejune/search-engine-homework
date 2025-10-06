package org.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CrawlStat {
    private Map<String, Integer> fetchStats;
    private List<VisitStat> visitStats;
    private Map<String, String> urlStats;

    public CrawlStat() {
        this.fetchStats = new ConcurrentHashMap<>();
        this.visitStats = new ArrayList<>();
        this.urlStats = new ConcurrentHashMap<>();
    }

    public void addFetchStat(String url, int statusCode) {
        fetchStats.put(url, statusCode);
    }

    public void addVisitStat(VisitStat stat) {
        visitStats.add(stat);
    }

    public void addUrlStat(String url, String indicator) {
        urlStats.put(url, indicator);
    }

    public Map<String, Integer> getFetchStats() {
        return fetchStats;
    }

    public List<VisitStat> getVisitStats() {
        return visitStats;
    }

    public Map<String, String> getUrlStats() {
        return urlStats;
    }
}
