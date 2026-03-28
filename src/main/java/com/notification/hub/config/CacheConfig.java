package com.notification.hub.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Cache configuration for template and preference caching.
 *
 * <p>Provides two cache manager implementations based on configuration:</p>
 * <ul>
 *   <li><b>Redis CacheManager</b> (production): Distributed caching with configurable TTLs</li>
 *   <li><b>Simple CacheManager</b> (development): In-memory caching when Redis is not available</li>
 * </ul>
 *
 * <h3>Cache Names and TTLs (Redis only)</h3>
 * <table border="1">
 *   <tr><th>Cache Name</th><th>TTL</th><th>Purpose</th></tr>
 *   <tr><td>templates</td><td>30 minutes</td><td>Notification templates</td></tr>
 *   <tr><td>preferences</td><td>5 minutes</td><td>User notification preferences</td></tr>
 *   <tr><td>notifications</td><td>10 minutes (default)</td><td>Notification data</td></tr>
 * </table>
 *
 * <h3>Configuration</h3>
 * <p>To enable Redis caching, set:</p>
 * <pre>
 * spring:
 *   data:
 *     redis:
 *       enabled: true
 * </pre>
 *
 * @see RedisConfig
 */
@Configuration
@Slf4j
public class CacheConfig {

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(10);
    private static final Duration TEMPLATES_TTL = Duration.ofMinutes(30);
    private static final Duration PREFERENCES_TTL = Duration.ofMinutes(5);

    private static final String[] CACHE_NAMES = {"templates", "preferences", "notifications"};

    /**
     * Redis-backed cache manager for production use.
     *
     * <p>Provides distributed caching with per-cache TTL configuration:</p>
     * <ul>
     *   <li>templates: 30 minutes</li>
     *   <li>preferences: 5 minutes</li>
     *   <li>default: 10 minutes</li>
     * </ul>
     *
     * @param connectionFactory the Redis connection factory
     * @return the Redis cache manager
     */
    @Bean
    @Primary
    @ConditionalOnProperty(
        name = "spring.data.redis.enabled",
        havingValue = "true"
    )
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        // Default cache configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(DEFAULT_TTL)
                .disableCachingNullValues()
                .serializeValuesWith(
                    RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer()
                    )
                );

        // Per-cache TTL configuration
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Templates cache: 30 minutes TTL
        cacheConfigurations.put("templates",
            defaultConfig.entryTtl(TEMPLATES_TTL));

        // Preferences cache: 5 minutes TTL
        cacheConfigurations.put("preferences",
            defaultConfig.entryTtl(PREFERENCES_TTL));

        // Notifications cache: default TTL
        cacheConfigurations.put("notifications",
            defaultConfig.entryTtl(DEFAULT_TTL));

        RedisCacheManager cacheManager = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();

        log.info("Cache manager: Redis (templates=30m, preferences=5m, default=10m)");
        return cacheManager;
    }

    /**
     * Simple in-memory cache manager for development use.
     *
     * <p>Used when Redis is not configured or explicitly disabled.
     * Provides local caching using {@link ConcurrentMapCacheManager}.</p>
     *
     * <p><b>Note:</b> This cache manager does not support TTL expiration.
     * Cache entries remain until the application restarts or are manually evicted.</p>
     *
     * @return the simple cache manager
     */
    @Bean
    @ConditionalOnProperty(
        name = "spring.data.redis.enabled",
        havingValue = "false",
        matchIfMissing = true
    )
    public CacheManager simpleCacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager(CACHE_NAMES);
        log.info("Cache manager: in-memory (Redis not configured)");
        return cacheManager;
    }
}
