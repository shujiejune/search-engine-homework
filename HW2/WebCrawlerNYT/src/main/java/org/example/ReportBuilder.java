package org.example;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/* This report builder was written by Gemini. */
public class ReportBuilder {

    public static void main(String[] args) {
        String fetchCsvFile = "fetch_nytimes.csv";
        String visitCsvFile = "visit_nytimes.csv";
        String urlsCsvFile = "urls_nytimes.csv";
        String reportFile = "CrawlReport_nytimes.txt";

        try {
            // --- Data Collection Structures ---
            int fetchesAttempted = 0;
            int fetchesSucceeded = 0;
            Map<Integer, Integer> statusCodes = new HashMap<>();

            long totalUrlsExtracted = 0;
            int[] fileSizeCounts = new int[5]; // Array to hold counts for 5 size categories
            Map<String, Integer> contentTypes = new HashMap<>();

            int uniqueUrls = 0;
            int uniqueUrlsWithin = 0;
            int uniqueUrlsOutside = 0;

            // --- Process fetch_nytimes.csv ---
            try (BufferedReader br = new BufferedReader(new FileReader(fetchCsvFile))) {
                String line;
                br.readLine(); // Skip header row
                while ((line = br.readLine()) != null) {
                    fetchesAttempted++;
                    String[] values = line.split(",");
                    if (values.length >= 2) {
                        int statusCode = Integer.parseInt(values[1]);
                        if (statusCode >= 200 && statusCode < 300) {
                            fetchesSucceeded++;
                        }
                        statusCodes.put(statusCode, statusCodes.getOrDefault(statusCode, 0) + 1);
                    }
                }
            }

            // --- Process visit_nytimes.csv ---
            try (BufferedReader br = new BufferedReader(new FileReader(visitCsvFile))) {
                String line;
                br.readLine(); // Skip header row
                while ((line = br.readLine()) != null) {
                    String[] values = line.split(",");
                    if (values.length >= 4) {
                        long size = Long.parseLong(values[1]);
                        totalUrlsExtracted += Long.parseLong(values[2]);
                        String contentType = values[3];

                        // File Size Categories
                        if (size < 1024) fileSizeCounts[0]++;
                        else if (size < 10240) fileSizeCounts[1]++;
                        else if (size < 102400) fileSizeCounts[2]++;
                        else if (size < 1048576) fileSizeCounts[3]++;
                        else fileSizeCounts[4]++;

                        contentTypes.put(contentType, contentTypes.getOrDefault(contentType, 0) + 1);
                    }
                }
            }

            // --- Process urls_nytimes.csv ---
            try (BufferedReader br = new BufferedReader(new FileReader(urlsCsvFile))) {
                String line;
                br.readLine(); // Skip header row
                while ((line = br.readLine()) != null) {
                    uniqueUrls++;
                    String[] values = line.split(",");
                    if (values.length >= 2) {
                        if ("OK".equals(values[1])) {
                            uniqueUrlsWithin++;
                        } else {
                            uniqueUrlsOutside++;
                        }
                    }
                }
            }

            // --- Build the Report String ---
            StringBuilder report = new StringBuilder();
            report.append("Name: Shujie Chen\n");
            report.append("USC ID: 7181302574\n");
            report.append("News site crawled: nytimes.com\n");
            report.append("Number of threads: 7\n\n");

            report.append("Fetch Statistics\n");
            report.append("================\n");
            report.append("# fetches attempted: ").append(fetchesAttempted).append("\n");
            report.append("# fetches succeeded: ").append(fetchesSucceeded).append("\n");
            report.append("# fetches failed or aborted: ").append(fetchesAttempted - fetchesSucceeded).append("\n\n");

            report.append("Outgoing URLs:\n");
            report.append("==============\n");
            report.append("Total URLs extracted: ").append(totalUrlsExtracted).append("\n");
            report.append("# unique URLs extracted: ").append(uniqueUrls).append("\n");
            report.append("# unique URLs within News Site: ").append(uniqueUrlsWithin).append("\n");
            report.append("# unique URLs outside News Site: ").append(uniqueUrlsOutside).append("\n\n");

            report.append("Status Codes:\n");
            report.append("=============\n");
            Map<Integer, Integer> sortedStatusCodes = new TreeMap<>(statusCodes);
            for (Map.Entry<Integer, Integer> entry : sortedStatusCodes.entrySet()) {
                report.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
            report.append("\n");

            report.append("File Sizes:\n");
            report.append("===========\n");
            report.append("< 1KB: ").append(fileSizeCounts[0]).append("\n");
            report.append("1KB ~ <10KB: ").append(fileSizeCounts[1]).append("\n");
            report.append("10KB ~ <100KB: ").append(fileSizeCounts[2]).append("\n");
            report.append("100KB ~ <1MB: ").append(fileSizeCounts[3]).append("\n");
            report.append(">= 1MB: ").append(fileSizeCounts[4]).append("\n\n");

            report.append("Content Types:\n");
            report.append("==============\n");
            Map<String, Integer> sortedContentTypes = new TreeMap<>(contentTypes);
            for (Map.Entry<String, Integer> entry : sortedContentTypes.entrySet()) {
                report.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }

            // --- Print to Console and Write to File ---
            System.out.println(report.toString());
            try (FileWriter writer = new FileWriter(reportFile)) {
                writer.write(report.toString());
            }
            System.out.println("\nReport successfully generated and saved to " + reportFile);

        } catch (IOException e) {
            System.err.println("Error reading or writing files: " + e.getMessage());
            e.printStackTrace();
        } catch (NumberFormatException e) {
            System.err.println("Error parsing a number from a CSV file. Please check the file format.");
            e.printStackTrace();
        }
    }
}

