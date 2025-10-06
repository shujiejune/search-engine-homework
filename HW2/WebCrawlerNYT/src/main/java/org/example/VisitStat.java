package org.example;

public class VisitStat {
    private int size;
    private int outlinks;
    private String contentType;

    public VisitStat(int size, int outlinks, String contentType) {
        this.size = size;
        this.outlinks = outlinks;
        this.contentType = contentType;
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
