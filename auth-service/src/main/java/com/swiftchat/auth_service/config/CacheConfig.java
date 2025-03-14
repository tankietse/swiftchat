package com.swiftchat.auth_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis cache configuration.
 * Sets up the cache manager and Redis serializers.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${spring.cache.redis.time-to-live}")
    private long defaultTtl;

    /**
     * Cache names with their TTLs
     */
    private static final Map<String, Duration> CACHE_TTL_MAP = new HashMap<>() {
        {
            put("users", Duration.ofMinutes(30));
            put("roles", Duration.ofMinutes(60));
            put("tokens", Duration.ofMinutes(10));
        }
    };

    /**
     * Configures Redis cache manager with appropriate serialization and TTLs.
     *
     * @param connectionFactory Redis connection factory
     * @return Configured CacheManager for Redis
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMillis(defaultTtl))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues();

        // Build configurations map with custom TTLs
        Map<String, RedisCacheConfiguration> configMap = new HashMap<>();
        CACHE_TTL_MAP.forEach((cacheName, ttl) -> configMap.put(cacheName, defaultConfig.entryTtl(ttl)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(configMap)
                .build();
    }
}
