package com.ids.repository;
import com.ids.model.*;
import com.ids.config.*;

import java.sql.*;
import java.util.*;

public class LogRepository {
    private static final String INSERT_SQL =
            "INSERT INTO network_traffic (flow_id, source_ip, destination_ip, source_port, " +
                    "destination_port, protocol, label, flow_timestamp, flow_statistics) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb)";

    /**
     * Inserts a list of NetworkLogs into the database using high-speed batching.
     */
    public void insertBatch(List<NetworkLog> logs) {

        // 1. Borrow a connection from the Hikari pool
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(INSERT_SQL)) {

            // 2. Disable Auto-Commit for Transaction Management
            conn.setAutoCommit(false);

            // 3. Loop through the data and build the batch
            for (NetworkLog log : logs) {
                pstmt.setString(1, log.getFlowId());
                pstmt.setString(2, log.getSourceIp());
                pstmt.setString(3, log.getDestinationIp());
                pstmt.setInt(4, log.getSourcePort());
                pstmt.setInt(5, log.getDestinationPort());
                pstmt.setString(6, log.getProtocol());
                pstmt.setString(7, log.getLabel());
                pstmt.setTimestamp(8, log.getFlowTimestamp());
                pstmt.setString(9, log.getFlowStatisticsJson()); // Our squashed JSON string

                pstmt.addBatch(); // "Add to the bucket"
            }

            // 4. Execute the entire batch in one network trip
            int[] results = pstmt.executeBatch();

            // 5. Commit the transaction (Make it permanent)
            conn.commit();

            System.out.println(" Successfully inserted a batch of " + results.length + " logs.");

        } catch (SQLException e) {
            System.err.println(" Batch insertion failed! Rolling back transaction.");
            e.printStackTrace();
            // In a real system, you would catch the connection and call conn.rollback() here,
            // but try-with-resources handles basic cleanup. We keep it simple for now.
        }
    }
}
