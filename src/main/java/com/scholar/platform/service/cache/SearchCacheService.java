package com.scholar.platform.service.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class SearchCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final Duration TTL = Duration.ofMinutes(5);

    @SuppressWarnings("unchecked")
    public <T> CachedPage<T> get(String key) {
        Object value = redisTemplate.opsForValue().get(namespaced(key));
        if (value instanceof CachedPage<?> cached) {
            return (CachedPage<T>) cached;
        }
        return null;
    }

    public void put(String key, CachedPage<?> page) {
        redisTemplate.opsForValue().set(namespaced(key), page, TTL);
    }

    public void evict(String key) {
        redisTemplate.delete(namespaced(key));
    }

    private String namespaced(String key) {
        return "search::" + key;
    }
}
