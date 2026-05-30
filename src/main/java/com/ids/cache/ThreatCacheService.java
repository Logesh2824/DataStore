package com.ids.cache;

import com.ids.config.DatabaseConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ThreatCacheService {

    private static JedisPool jedisPool;

    static {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        jedisPool = new JedisPool(poolConfig, "localhost", 6379);
        System.out.println("✅ Redis Cache Pool Initialized.");
    }

    /**
     * The Master Cache-Aside Method
     */
    public static boolean isIpMalicious(String ipAddress) {
        try (Jedis jedis = jedisPool.getResource()) {

            // 1. Check Redis Cache
            String cachedResult = jedis.get("threat_ip:" + ipAddress);

            if (cachedResult != null) {
                // CACHE HIT: Return instantly
                return Boolean.parseBoolean(cachedResult);
            }

            // 2. CACHE MISS: Fallback to PostgreSQL
            boolean isMaliciousFromDb = checkDatabaseForThreat(ipAddress);

            // 3. Update Redis for next time (1-hour expiration)
            jedis.setex("threat_ip:" + ipAddress, 3600, String.valueOf(isMaliciousFromDb));

            return isMaliciousFromDb;

        } catch (Exception e) {
            System.err.println("Redis connection failed! Falling back to DB only.");
            // If Redis crashes, our system doesn't die. It just slows down by hitting the DB directly.
            return checkDatabaseForThreat(ipAddress);
        }
    }

    /**
     * The JDBC Fallback Method
     */
    private static boolean checkDatabaseForThreat(String ipAddress) {
        // We only select '1' (a simple boolean flag) because it's faster than selecting the whole row
        String sql = "SELECT 1 FROM threat_intelligence WHERE ip_address = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, ipAddress);

            try (ResultSet rs = pstmt.executeQuery()) {
                // If rs.next() is true, a row was found, meaning the IP is in our threat database.
                return rs.next();
            }

        } catch (SQLException e) {
            System.err.println("❌ Database query failed for Threat Intel lookup.");
            e.printStackTrace();
            return false; // Fail-safe: assume benign if our DB is unreachable
        }
    }

    public static void closePool() {
        if (jedisPool != null) {
            jedisPool.close();
        }
    }
}