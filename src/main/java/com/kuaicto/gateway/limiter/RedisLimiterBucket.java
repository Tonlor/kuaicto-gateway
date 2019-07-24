package com.kuaicto.gateway.limiter;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands.SetOption;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.types.Expiration;


public class RedisLimiterBucket {

    private StringRedisTemplate redisTemplate;

    public RedisLimiterBucket(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    public Long increment(String key, long delta) {
        return redisTemplate.boundValueOps(key).increment(delta);
    }
    
    public void expire(String key, long expires) {
        redisTemplate.expire(key, expires, TimeUnit.MILLISECONDS);
    }
    
    public void set(String key, String value, long expires) {
        redisTemplate.opsForValue().set(key, value, expires, TimeUnit.MILLISECONDS);
    }
    
    public boolean setIfAbsent(String key, String value, long expires) {
        RedisConnection connection = redisTemplate.getConnectionFactory().getConnection();
        Boolean b = connection.set(key.getBytes(), value.getBytes(), Expiration.milliseconds(expires), SetOption.ifAbsent());
        return b;
    }
}
