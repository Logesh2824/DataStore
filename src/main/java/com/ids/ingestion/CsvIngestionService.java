package com.ids.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ids.cache.ThreatCacheService;
import com.ids.model.NetworkLog;
import com.ids.repository.LogRepository;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CsvIngestionService {

    private final LogRepository repository;
    private final ObjectMapper objectMapper; // Jackson JSON converter

    // The CIC-IDS dataset uses this date format (dd/MM/yyyy HH:mm:ss)
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    public CsvIngestionService() {
        this.repository = new LogRepository();
        this.objectMapper = new ObjectMapper();
    }

    public void processCsv(String fileName) {
        System.out.println(" Starting Ingestion for: " + fileName);

        List<NetworkLog> batchList = new ArrayList<>();
        int batchSize = 5000; // Dump to DB every 5,000 rows
        int totalProcessed = 0;

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(fileName);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

            if (is == null) throw new RuntimeException("Cannot find file: " + fileName);

            // Read the Header row to get column names for our JSON keys
            String headerLine = reader.readLine();
            String[] headers = headerLine.split(",");

            String line;
            while ((line = reader.readLine()) != null) {
                String[] values = line.split(",");

                // 1. Extract Core Columns
                String flowId = values[0];
                String sourceIp = values[1];
                int sourcePort = Integer.parseInt(values[2]);
                String destIp = values[3];
                int destPort = Integer.parseInt(values[4]);
                String protocol = values[5];

                // Parse the specific CIC-IDS timestamp format into a SQL Timestamp
                Timestamp timestamp = new Timestamp(dateFormat.parse(values[6]).getTime());

                // Label is always the last column
                String label = values[values.length - 1];

                // 2. Pack everything else into the JSON Superpower Map
                Map<String, String> statsMap = new HashMap<>();
                for (int i = 7; i < values.length - 1; i++) {
                    statsMap.put(headers[i], values[i]);
                }

                // 3. Convert Map to JSON String using Jackson
                String jsonStats = objectMapper.writeValueAsString(statsMap);

                // 4. Create the Java Object and add to our bucket
                NetworkLog log = new NetworkLog(
                        flowId, sourceIp, destIp, sourcePort, destPort,
                        protocol, label, timestamp, jsonStats
                );
                boolean isKnownThreat = ThreatCacheService.isIpMalicious(sourceIp);
                if (isKnownThreat) {
                    System.out.println("🚨 ALERT: Ingesting traffic from known malicious IP: " + sourceIp);
                    // You could theoretically modify the 'label' or add a flag to your JSON payload here
                }
                batchList.add(log);
                totalProcessed++;

                // 5. If bucket is full, execute the JDBC Batch Insert!
                if (batchList.size() >= batchSize) {
                    repository.insertBatch(batchList);
                    batchList.clear(); // Empty the bucket for the next batch
                }
            }

            // Insert any remaining logs that didn't perfectly hit the 5,000 limit
            if (!batchList.isEmpty()) {
                repository.insertBatch(batchList);
            }

            System.out.println(" Ingestion Complete! Total rows processed: " + totalProcessed);

        } catch (Exception e) {
            System.err.println(" Error processing CSV!");
            e.printStackTrace();
        }
    }
}