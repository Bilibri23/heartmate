package org.rooms.roombay.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisConfig {
    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    /**
     * Redis connection factory - only created if Redis is enabled
     */
    @Bean
    @ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = false)
    public JedisConnectionFactory jedisConnectionFactory() {
        try {
            RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
            config.setHostName(redisHost);
            config.setPort(redisPort);
            if (StringUtils.hasText(redisPassword)) {
                config.setPassword(redisPassword);
            }
            JedisConnectionFactory factory = new JedisConnectionFactory(config);
            factory.afterPropertiesSet();
            factory.getConnection().ping();
            log.info("Redis connection established successfully");
            return factory;
        } catch (Exception e) {
            log.error("Failed to connect to Redis: {}", e.getMessage());
            throw new RuntimeException("Redis connection failed", e);
        }
    }
    
    /**
     * Redis template - only created if Redis is enabled
     */
    @Bean
    @ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = false)
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
    
    /**
     * Default in-memory cache manager (always available)
     */
    @Bean(name = "cacheManager")
    public CacheManager inMemoryCacheManager() {
        log.info("Using in-memory cache manager");
        return new org.springframework.cache.concurrent.ConcurrentMapCacheManager(
            "listings", "recommendations", "matches", "profiles", "notifications"
        );
    }
    
    /**
     * Redis cache manager - only created if Redis is enabled (takes precedence)
     */
    @Bean(name = "cacheManager")
    @Primary
    @ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = false)
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(5))
            .serializeKeysWith(org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
            .disableCachingNullValues();
        
        log.info("Using Redis cache manager");
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withCacheConfiguration("listings", defaultConfig.entryTtl(Duration.ofMinutes(5)))
            .withCacheConfiguration("recommendations", defaultConfig.entryTtl(Duration.ofMinutes(15)))
            .withCacheConfiguration("matches", defaultConfig.entryTtl(Duration.ofMinutes(30)))
            .withCacheConfiguration("profiles", defaultConfig.entryTtl(Duration.ofMinutes(10)))
            .withCacheConfiguration("notifications", defaultConfig.entryTtl(Duration.ofMinutes(1)))
            .transactionAware()
            .build();
    }
}
