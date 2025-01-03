package com.extazy.web.manager;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * 多级缓存
 */
@Component
@Slf4j
public class CacheManager {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 本地缓存（<String, CacheValue>）
     */
    private final Cache<String, CacheValue> localCache = Caffeine.newBuilder()
            .expireAfterWrite(100, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build();

    private static final String REDIS_VERSION_PREFIX = "cache:version:";
    private static final String REDIS_DATA_PREFIX = "cache:data:";
    private static final long CACHE_EXPIRE_MINUTES = 100L;

    /**
     * 写入缓存
     */
    public void put(String key, Object value) {
        if (key == null) {
            return;
        }

        // 1) 先做一次安全的 getVersion，然后 +1
        long oldVersion = getSafeVersion(key);
        long newVersion = oldVersion + 1;

        // 2) 写回 Redis 版本号（这里存的是 newVersion，保证是 long）
        redisTemplate.opsForValue().set(getVersionKey(key), newVersion);

        // 3) 根据新版本号，拼接真正的数据键
        String realDataKey = getDataKey(key, newVersion);
        redisTemplate.opsForValue().set(realDataKey, value, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);

        // 4) 写入本地缓存（Key 拼上最新版本号），也用 Long
        String localKey = getLocalKey(key, newVersion);
        CacheValue cacheValue = new CacheValue(value, newVersion);
        localCache.put(localKey, cacheValue);
    }

    /**
     * 读缓存
     */
    public Object get(String key) {
        if (key == null) {
            return null;
        }
        // 1) 读出 Redis 最新版本号（确保是 long）
        long redisVersion = getSafeVersion(key);
        if (redisVersion <= 0) {
            // 说明还没写入过
            return null;
        }

        // 2) 拿到本地缓存 key
        String localKey = getLocalKey(key, redisVersion);
        CacheValue localValue = localCache.getIfPresent(localKey);
        if (localValue != null) {
            // 有值且版本一致，直接返回
            return localValue.getData();
        }

        // 3) 不一致或本地无缓存，则从 Redis 取
        String realDataKey = getDataKey(key, redisVersion);
        Object redisValue = redisTemplate.opsForValue().get(realDataKey);
        if (redisValue != null) {
            // 回填本地缓存
            CacheValue newCacheValue = new CacheValue(redisValue, redisVersion);
            localCache.put(localKey, newCacheValue);
        }
        return redisValue;
    }

    /**
     * 删除缓存
     */
    public void delete(String key) {
        if (key == null) {
            return;
        }
        // 1) 获取 Redis 中最新版本号（安全方式）
        long redisVersion = getSafeVersion(key);
        if (redisVersion > 0) {
            // 2) 删除 Redis 中的数据
            String realDataKey = getDataKey(key, redisVersion);
            redisTemplate.delete(realDataKey);

            // 3) 删除本地缓存
            String localKey = getLocalKey(key, redisVersion);
            localCache.invalidate(localKey);
        }
        // 4) 最后删除版本号
        redisTemplate.delete(getVersionKey(key));
    }

    /**
     * 统一获取 version，避免 Integer / Long 混用
     */
    private long getSafeVersion(String key) {
        Object versionObj = redisTemplate.opsForValue().get(getVersionKey(key));
        if (versionObj instanceof Number) {
            return ((Number) versionObj).longValue();
        }
        return 0L;
    }

    /**
     * 拼接版本号键
     */
    private String getVersionKey(String key) {
        return REDIS_VERSION_PREFIX + key;
    }

    /**
     * 拼接数据键
     */
    private String getDataKey(String key, long version) {
        return REDIS_DATA_PREFIX + key + ":" + version;
    }

    /**
     * 拼接本地缓存键（带版本号）
     */
    private String getLocalKey(String key, long version) {
        return key + ":" + version;
    }

    /**
     * 本地缓存的数据容器
     */
    private static class CacheValue {
        private final Object data;
        private final long version;

        public CacheValue(Object data, long version) {
            this.data = data;
            this.version = version;
        }
        public Object getData() {
            return data;
        }
        public long getVersion() {
            return version;
        }
    }

    public RedisTemplate<String, Object> getRedisTemplate() {
        return this.redisTemplate;
    }
}