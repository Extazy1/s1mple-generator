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
     * 本地缓存（仅存 {key, CacheValue}，其中 CacheValue 里含有具体数据和 version）
     */
    private final Cache<String, CacheValue> localCache = Caffeine.newBuilder()
            .expireAfterWrite(100, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build();

    /**
     * 缓存版本号在 Redis 中的前缀
     */
    private static final String REDIS_VERSION_PREFIX = "cache:version:";

    /**
     * 缓存数据在 Redis 中的前缀
     */
    private static final String REDIS_DATA_PREFIX = "cache:data:";

    /**
     * 过期时间
     */
    private static final long CACHE_EXPIRE_MINUTES = 100L;

    /**
     * 写入缓存
     * 1. 递增并获取最新版本号
     * 2. 根据版本号写入 Redis
     * 3. 写入本地缓存
     *
     * @param key   业务键
     * @param value 要缓存的值
     */
    public void put(String key, Object value) {
        if (key == null) {
            return;
        }
        // 1) 递增 Redis 中的版本号
        Long newVersion = redisTemplate.opsForValue().increment(getVersionKey(key));
        if (newVersion == null) {
            // 如果 Redis 出故障，可以做降级处理；这里简单返回
            log.error("Redis increment version failed, key = {}", key);
            return;
        }
        // 2) 根据版本号写入 Redis
        String realDataKey = getDataKey(key, newVersion);
        redisTemplate.opsForValue().set(realDataKey, value, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);

        // 3) 写入本地缓存（记录数据和最新版本号）
        CacheValue cacheValue = new CacheValue(value, newVersion);
        localCache.put(key, cacheValue);
    }

    /**
     * 读缓存
     * 1. 先从 Redis 获取最新版本号
     * 2. 本地缓存若版本号一致，则直接返回
     * 3. 否则从 Redis 获取最新值，回填本地缓存
     *
     * @param key 业务键
     * @return 对应的值 or null
     */
    public Object get(String key) {
        if (key == null) {
            return null;
        }
        // 1) 获取 Redis 中的最新版本号
        Object versionObj = redisTemplate.opsForValue().get(getVersionKey(key));
        if (versionObj == null) {
            // 版本号都没有，说明还没写入过，可能缓存不存在
            return null;
        }
        long redisVersion;
        try {
            redisVersion = Long.parseLong(versionObj.toString());
        } catch (NumberFormatException e) {
            log.error("version parse error, key = {}, version = {}", key, versionObj);
            return null;
        }

        // 2) 查看本地缓存里的缓存值
        CacheValue localValue = localCache.getIfPresent(key);
        if (localValue != null && localValue.getVersion() == redisVersion) {
            // 版本一致，直接返回本地缓存的数据
            return localValue.getData();
        }

        // 3) 版本不一致或本地无缓存，则从 Redis 重新获取
        String realDataKey = getDataKey(key, redisVersion);
        Object redisValue = redisTemplate.opsForValue().get(realDataKey);
        if (redisValue != null) {
            // 回填本地缓存
            CacheValue newCacheValue = new CacheValue(redisValue, redisVersion);
            localCache.put(key, newCacheValue);
        }
        return redisValue;
    }

    /**
     * 删除缓存
     * 1. 删除本地缓存
     * 2. 删除 Redis 中版本号键 + 最新数据键
     *
     * @param key 业务键
     */
    public void delete(String key) {
        if (key == null) {
            return;
        }
        // 1) 删除本地缓存
        localCache.invalidate(key);

        // 2) 获取 Redis 中最新版本号
        Object versionObj = redisTemplate.opsForValue().get(getVersionKey(key));
        if (versionObj != null) {
            long redisVersion;
            try {
                redisVersion = Long.parseLong(versionObj.toString());
            } catch (NumberFormatException e) {
                log.error("delete parse version error, key = {}, version = {}", key, versionObj);
                return;
            }
            // 删除 data
            String realDataKey = getDataKey(key, redisVersion);
            redisTemplate.delete(realDataKey);
        }
        // 删除 version
        redisTemplate.delete(getVersionKey(key));
    }

    /**
     * 拼接版本号键
     * 形如：cache:version:{key}
     *
     * @param key 原始业务键
     * @return redis 中存放 version 的键
     */
    private String getVersionKey(String key) {
        return REDIS_VERSION_PREFIX + key;
    }

    /**
     * 拼接数据键
     * 形如：cache:data:{key}:{version}
     *
     * @param key     原始业务键
     * @param version 缓存版本号
     * @return redis 中实际存放数据的键
     */
    private String getDataKey(String key, long version) {
        return REDIS_DATA_PREFIX + key + ":" + version;
    }

    /**
     * 用于本地缓存的数据容器，包含“真正的数据”和“版本号”
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

}
