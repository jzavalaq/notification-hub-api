package com.notification.hub.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis configuration for distributed caching.
 *
 * <p>This configuration is only activated when Redis is explicitly enabled
 * via the {@code spring.data.redis.enabled=true} property.</p>
 *
 * <p>In production, Redis provides distributed caching across multiple instances.
 * In development, when Redis is disabled, the application gracefully falls back
 * to in-memory caching via {@link CacheConfig}.</p>
 *
 * <h3>Configuration</h3>
 * <p>Set the following properties to enable Redis:</p>
 * <pre>
 * spring:
 *   data:
 *     redis:
 *       enabled: true
 *       url: redis://localhost:6379
 * </pre>
 *
 * @see CacheConfig
 */
@Configuration
@ConditionalOnProperty(
    name = "spring.data.redis.enabled",
    havingValue = "true",
    matchIfMissing = false
)
@Slf4j
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    /**
     * Creates the Redis connection factory using Lettuce.
     *
     * @return the Lettuce connection factory
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        log.info("Redis connection configured: {}:{}", redisHost, redisPort);

        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);
        if (redisPassword != null && !redisPassword.isBlank()) {
            config.setPassword(redisPassword);
        }

        return new LettuceConnectionFactory(config);
    }

    /**
     * Creates the Redis template for cache operations.
     *
     * @param connectionFactory the Redis connection factory
     * @return the configured Redis template
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use String serializer for keys (human-readable in Redis)
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Use JSON serializer for values
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        log.info("Redis template configured with JSON serialization");
        return template;
    }
}
