package com.ids.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class DatabaseConfig {
    private static HikariDataSource dataSource;

    static {
        try {
            Properties props = new Properties();
            try (InputStream input = DatabaseConfig.class.getClassLoader().getResourceAsStream("database.properties")) {
                if (input == null) {
                    throw new RuntimeException("Unable to find properties");
                }
                props.load(input);
            }

            HikariConfig config= new HikariConfig();
            config.setJdbcUrl(props.getProperty("db.url"));
            config.setUsername(props.getProperty("db.username"));
            config.setPassword(props.getProperty("db.password"));

            int poolSize = Integer.parseInt(props.getProperty("db.pool.max_size","10"));
            config.setMaximumPoolSize(poolSize);
            config.setMinimumIdle(5); // Always keep 5 connections alive and ready
            config.setConnectionTimeout(30000); // Wait 30 seconds max for a connection
            config.setIdleTimeout(600000);

            dataSource =new HikariDataSource(config);
            System.out.println("New Connection pool initiated");
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to init connection pool");

        }
    }
    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
    public static void closePool() {
        if (dataSource != null) {
            dataSource.close();
            System.out.println("Connection pool closed.");
        }
    }

    }
