package com.tlavu.linkforge.infrastructure.redis;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.RedisConnection;

/**
 * Standalone Utility to clean Redis using environment variables directly.
 * This avoids starting the full Spring Context and its dependencies (DB, Mail, etc.)
 */
class RedisCleaningUtility {

    @Test
    void flushRedis() {
        System.out.println("--------------------------------------------------------");
        System.out.println("REDIS STANDALONE CLEANING UTILITY");
        
        // Read env variables directly
        String host = System.getenv("REDIS_HOST");
        String portStr = System.getenv("REDIS_PORT");
        String password = System.getenv("REDIS_PASSWORD");
        String sslEnabled = System.getenv("REDIS_SSL_ENABLED");

        if (host == null || portStr == null) {
            System.err.println("ERROR: REDIS_HOST or REDIS_PORT not found in environment.");
            return;
        }

        int port = Integer.parseInt(portStr);
        System.out.println("Target:  " + host + ":" + port + (Boolean.parseBoolean(sslEnabled) ? " (SSL)" : ""));

        // Configure Redis Standalone
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(host, port);
        if (password != null && !password.isEmpty()) {
            config.setPassword(password);
        }

        // Configure Lettuce with SSL if needed
        LettuceClientConfiguration clientConfig = Boolean.parseBoolean(sslEnabled) 
            ? LettuceClientConfiguration.builder().useSsl().build()
            : LettuceClientConfiguration.defaultConfiguration();

        LettuceConnectionFactory factory = new LettuceConnectionFactory(config, clientConfig);
        factory.afterPropertiesSet(); // Initialize factory

        try {
            factory.start();
            RedisConnection connection = factory.getConnection();
            connection.serverCommands().flushAll();
            System.out.println("SUCCESS: Redis flushed successfully!");
            connection.close();
        } catch (Exception e) {
            System.err.println("ERROR: Failed to flush Redis: " + e.getMessage());
        } finally {
            factory.destroy();
        }
        
        System.out.println("--------------------------------------------------------");
    }
}
