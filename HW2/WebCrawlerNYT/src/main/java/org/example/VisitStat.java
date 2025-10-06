package org.example;

public class VisitStat {
    private String url;
    private int size;
    private int outlinks;
    private String contentType;

    public VisitStat(String url, int size, int outlinks, String contentType) {
        this.url = url;
        this.size = size;
        this.outlinks = outlinks;
        this.contentType = contentType;
    }

    public String getUrl() {
        return url;
    }

    public int getSize() {
        return size;
    }

    public int getOutlinks() {
        return outlinks;
    }

    public String getContentType() {
        return contentType;
    }
}
