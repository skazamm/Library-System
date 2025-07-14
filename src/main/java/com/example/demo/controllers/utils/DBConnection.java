package com.example.demo.controllers.utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class DBConnection {
    // Only one pool per app
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://aws-0-us-west-1.pooler.supabase.com:6543/postgres");
        config.setUsername("postgres.wptragsangephgqircna");
        config.setPassword("new09293949801");
        config.setMaximumPoolSize(10); // Adjust as needed
        config.setMinimumIdle(2);
        config.setIdleTimeout(60000); // 60s
        config.setMaxLifetime(300000); // 5min
        config.setConnectionTimeout(10000); // 10s

        // Optional: recommended for PostgreSQL
        config.addDataSourceProperty("prepareThreshold", "0");
        config.addDataSourceProperty("tcpKeepAlive", "true");
        // Optionally turn on leakDetectionThreshold to debug connection leaks
        // config.setLeakDetectionThreshold(10000); // 10s

        dataSource = new HikariDataSource(config);
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
