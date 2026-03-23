package com.blog.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.cache.annotation.EnableCaching;

import java.time.Duration;
import java.util.Map;

/**
 * I1: Redis cache configuration for hot articles, homepage data, and article details.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    @SuppressWarnings("null")
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigs = Map.of(
                "hotArticles", RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofMinutes(5)),
                "homepageData", RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofMinutes(3)),
                "articleDetail", RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofMinutes(15)),
                "seoConfig", RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofHours(1)),
                "sitemap", RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofHours(1))
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }
}
