package com.kuaicto.gateway.cache;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

public class ProfileCache {
    private static final Logger logger = LoggerFactory.getLogger(ProfileCache.class);
    
    private ValueOperations<String, String> valueOperations;
    
    public ProfileCache(StringRedisTemplate redisTemplate) {
        valueOperations = redisTemplate.opsForValue();
    }

    public String get(String key) {
        try {
            return valueOperations.get(redisKey(key));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    public void set(String key, String userInfo, long expires) {
        try {
            valueOperations.set(redisKey(key), userInfo, expires, TimeUnit.MILLISECONDS); // no need lock here        
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
    
    private String redisKey(String cookie) {
        String key = "gw.cache.profile::" + cookie;
        return key;
    }
}
