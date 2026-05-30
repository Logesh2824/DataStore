package com.ids;

import com.ids.config.DatabaseConfig;
import com.ids.ingestion.CsvIngestionService;

public class Main {
    public static void main(String[] args) {

        System.out.println("Initializing Database Connection Pool...");
        try {
            DatabaseConfig.getConnection().close();
        } catch (Exception e) {
            System.err.println("Database is down. Aborting.");
            return;
        }

        // 1. Start the Stopwatch
        long startTime = System.currentTimeMillis();

        // 2. Run the Heavy Ingestion
        CsvIngestionService ingestionService = new CsvIngestionService();
        // Make sure this matches the exact filename you downloaded!
        ingestionService.processCsv("Friday-WorkingHours-Afternoon-DDos.pcap_ISCX.csv");

        // 3. Stop the Stopwatch
        long endTime = System.currentTimeMillis();
        long durationMs = endTime - startTime;

        // 4. Check JVM Memory Usage
        Runtime runtime = Runtime.getRuntime();
        runtime.gc(); // Politely ask Java to clean up garbage before we measure
        long memoryUsedBytes = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsedMb = memoryUsedBytes / (1024 * 1024);

        // 5. Print the Benchmark Report
        System.out.println("\n========================================");
        System.out.println("📊 STRESS TEST RESULTS");
        System.out.println("========================================");
        System.out.println("Time Taken:    " + (durationMs / 1000.0) + " seconds");
        System.out.println("RAM Consumed:  ~" + memoryUsedMb + " MB");
        System.out.println("Batch Size:    " + 5000 + " (Set in CsvIngestionService)");
        System.out.println("========================================");

        DatabaseConfig.closePool();
    }
}