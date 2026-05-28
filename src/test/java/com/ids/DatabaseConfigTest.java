package com.ids;

import com.ids.config.DatabaseConfig;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseConfigTest {
    @Test
    public void testDatabaseConnectionAndTable() {
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {

            // Assert we got a connection
            assertNotNull(conn, "Connection should not be null");

            // Assert table exists
            ResultSet rsTable = stmt.executeQuery("SELECT count(*) FROM network_traffic;");
            assertTrue(rsTable.next(), "Query should return a result");

            System.out.println("✅ JUnit Test Passed: Table exists with " + rsTable.getInt(1) + " rows.");
            rsTable.close();

        } catch (Exception e) {
            fail("Database connection or query failed: " + e.getMessage());
        } finally {
            DatabaseConfig.closePool();
        }
    }
}

