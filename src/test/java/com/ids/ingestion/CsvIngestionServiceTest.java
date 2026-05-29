package com.ids.ingestion;

import com.ids.config.DatabaseConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class CsvIngestionServiceTest {

    /**
     * @BeforeEach runs automatically before every single @Test.
     * We use it to wipe the database table clean.
     * This guarantees our test results are consistent, whether we run it 1 time or 100 times.
     */
    @BeforeEach
    public void setUpDatabaseCleanSlate() {
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {

            // TRUNCATE is a high-speed SQL command that instantly empties a table
            stmt.execute("TRUNCATE TABLE network_traffic;");
            System.out.println("🧹 Database cleaned for test.");

        } catch (Exception e) {
            fail("Failed to prepare the database for testing: " + e.getMessage());
        }
    }

    /**
     * The actual integration test for our CSV Ingestion Service.
     */
    @Test
    public void testProcessCsv_SuccessfullyInsertsData() {
        System.out.println("🧪 Running CSV Ingestion Test...");

        // 1. Execute the system under test
        CsvIngestionService service = new CsvIngestionService();
        service.processCsv("mini_cic_dataset.csv");

        // 2. Verify the outcome in the database
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM network_traffic;");

            if (rs.next()) {
                int rowCount = rs.getInt(1);

                // 3. The Assertion: We expect exactly 3 rows from our mini dataset
                assertEquals(3, rowCount, "The database should contain exactly 3 rows from the mini CSV.");
                System.out.println("✅ JUnit Assertion Passed: Expected 3 rows, found " + rowCount);
            } else {
                fail("Query did not return a count result.");
            }

        } catch (Exception e) {
            fail("Database verification failed during test: " + e.getMessage());
        }
    }

    /**
     * @AfterAll runs once after all tests have finished.
     * We use it to safely shut down the HikariCP pool.
     */
    @AfterAll
    public static void tearDown() {
        DatabaseConfig.closePool();
        System.out.println("🛑 Test suite finished. Connection pool closed.");
    }
}