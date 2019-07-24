package com.kuaicto.gateway.limiter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PreDestroy;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.alibaba.fastjson.JSON;
import com.kuaicto.gateway.alarm.ErrorAlarmService;
import com.kuaicto.gateway.alarm.ErrorMsgData;
import com.kuaicto.gateway.utils.NetworkUtils;

/**
 * 基于IP的限流
 */
public class RequestLimiterCenter implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(RequestLimiterCenter.class);
    private static final ExecutorService mainExecutor = Executors.newSingleThreadExecutor();
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    
    private static final int TIMEOUT_SECONDS = 30;

    public static final String MSG_DATA_QUQUE = "gw::RequestLimiterMsgData";
    private static final byte[] MSG_DATA_QUQUE_AS_BYTES = MSG_DATA_QUQUE.getBytes();

    private RequestLimiterConfig cfg;
    private StringRedisTemplate redisTemplate;
    private RedisLimiterBucket bucket;
    private AtomicBoolean isShutdown = new AtomicBoolean(false);
    
    private ErrorAlarmService alarmService;

    public RequestLimiterCenter(StringRedisTemplate redisTemplate, ErrorAlarmService alarmService) throws Exception {
        this.redisTemplate = redisTemplate;
        this.alarmService = alarmService;
        this.bucket = new RedisLimiterBucket(redisTemplate);
        this.cfg = RequestLimiterConfig.load();

        if (this.cfg.isEnabled()) {
            mainExecutor.execute(this);
            // new Thread(this).start();
        }
    }

    @Override
    public void run() {
        RedisConnection connection = redisTemplate.getConnectionFactory().getConnection();
        
        while (!isShutdown.get()) {
            try {
                if (connection.isClosed()) {
                    logger.info("redis connection closed. will get a new connection");
                    connection = redisTemplate.getConnectionFactory().getConnection();
                }
                List<byte[]> list = connection.bLPop(TIMEOUT_SECONDS, MSG_DATA_QUQUE_AS_BYTES);
                if (CollectionUtils.isEmpty(list)) {
                    continue;
                }
                
                String text = new String(list.get(1)); // 0->key, 1->value
                logger.trace("bLPop: {}", text);
                RequestLimiterMsgData msgData = JSON.parseObject(text, RequestLimiterMsgData.class);
                onRequestReceived(msgData, true);
            } catch (Exception e) {
                if (e.getCause() instanceof CancellationException) {
                    // ignore
                } else {
                    logger.error(e.getMessage(), e);
                }
            }
        }
        
        connection.close();
    }

    @PreDestroy
    public void shutdown() {
        isShutdown.set(true);
        
        mainExecutor.shutdown();
        executor.shutdown();
    }
    
    public RequestLimiterConfig getRequestLimiterConfig() {
        return this.cfg;
    }

    /**
     * 检查白名单
     * @param ip
     * @return
     */
    private boolean inWhitelist(String ip) {
        boolean b = cfg.getWhitelist().contains(ip);
        return b;
    }
    
    /**
     * 检查黑名单
     * @param ip
     * @return
     */
    private boolean inBlacklist(String ip) {
        // 检查黑名单(无代理)
        if (cfg.getBlacklist().contains(ip)) {
            return true;
        }
        // 检查黑名单(有代理)
        String[] arr = StringUtils.split(ip, ",");
        if (arr != null && arr.length > 1) {
            for (String s : arr) {
                String trimed = StringUtils.trim(s);
                if (StringUtils.isNotBlank(trimed) && cfg.getBlacklist().contains(trimed)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * 是否达到限流，被屏蔽
     * @param ip
     * @return
     */
    private boolean inBucketBlocked(RequestLimiterMsgData msgData) {
        final String ip = msgData.getIp();
        final String cookieValue = msgData.getCookie();
        final String resource = msgData.getResource(); 

        final String ipTopic = "ip=" + ip;
        final String cookieTopic = cookieValue == null ? null : "md5Hex(cookie)=" + md5Hex(cookieValue);
        
        List<String> keys = new ArrayList<>();
        keys.add(bucketBlockedKey(ipTopic));
        keys.add(bucketBlockedKey(ipTopic + resource));
        if (cookieTopic != null) {
            keys.add(bucketBlockedKey(cookieTopic));
            keys.add(bucketBlockedKey(cookieTopic + resource));
        }
        List<String> values = this.redisTemplate.opsForValue().multiGet(keys);
        logger.trace("inBucketBlocked, keys={}, values={}", keys, values);
        for (int i = 0; i < values.size(); i++) {
            String value = values.get(i);
            if (StringUtils.isNotBlank(value)) {
                logger.warn("TOO_MANY_REQUESTS [blocked]: ip: {}, path: {}, topic: {}, value: {}", ip, resource, keys.get(i), value);
                return true;
            }
        }

        return false;
    }
    
    /**
     * 处理限流检查
     * @param msgData
     * @return 是否通过限流检查: true是, false否 
     */
    public boolean onRequestReceived(RequestLimiterMsgData msgData) {
        // 是否启用限流
        if (!this.cfg.isEnabled()) {
            return true;
        }
        
        // 检查白名单
        if (inWhitelist(msgData.getIp())) {
            return true;
        }
        // 检查白名单
        if (inBlacklist(msgData.getIp())) {
            return false;
        }
        
        // 检查是否需要限流
        if (!matchLimiterRule(msgData, null)) {
            return true;
        }
        
        // 检查是否达到限流
        if (inBucketBlocked(msgData)) {
            return false;
        }
        
        // 限流检查(同步)
        if (!onRequestReceived(msgData, false)) {
            return false;
        }
        
        // 检查是否需要限流(异步)
        if (matchLimiterRule(msgData, true)) {
            // 限流检查(异步)
            executor.execute(new Runnable(){
                @Override
                public void run() {
                    String jsonString = JSON.toJSONString(msgData);
                    redisTemplate.boundListOps(RequestLimiterCenter.MSG_DATA_QUQUE).rightPush(jsonString);
                }
            });
        }
        
        return true;
    }
     
    /**
     * 接受请求并处理限流 
     * @param msgData
     * @param async 是否有异步检查
     * @return
     */
    private boolean onRequestReceived(RequestLimiterMsgData msgData, boolean async) {
        logger.trace("onRequestReceived: async={}, data={}", async, msgData);
        
        final String ip = msgData.getIp();
        final String resource = msgData.getResource();
        final String ipTopic = "ip=" + ip;
        final String cookieValue = msgData.getCookie();
        final String cookieTopic = cookieValue == null ? null : "md5Hex(cookie)=" + md5Hex(cookieValue);
        
        if (!this.checkRate(msgData, ipTopic, this.cfg.getRequestPerIp(), async)) {
            logger.warn("TOO_MANY_REQUESTS [RequestPerIp]: ip: {}, path: {}, topic: {}", ip, resource, ipTopic);
            return false;
        }
        if (!this.checkRate(msgData, ipTopic + resource, this.cfg.getResourcePerIp(), async)) {
            logger.warn("TOO_MANY_REQUESTS [ResourcePerIp]: ip: {}, path: {}, topic: {}", ip, resource, ipTopic);
            return false;
        }
        if (cookieTopic != null && !this.checkRate(msgData, cookieTopic, this.cfg.getRequestPerCookie(), async)) {
            logger.warn("TOO_MANY_REQUESTS [RequestPerCookie]: ip: {}, path: {}, topic: {}", ip, resource, cookieTopic);
            return false;
        }
        if (cookieTopic != null && !this.checkRate(msgData, cookieTopic + resource, this.cfg.getResourcePerCookie(), async)) {
            logger.warn("TOO_MANY_REQUESTS [ResourcePerCookie]: ip: {}, path: {}, topic: {}", ip, resource, cookieTopic);
            return false;
        }
        
        return true;
    }

    /**
     * 是否跳过限流检查
     * @param msgData
     * @param async 是否检查异步的规则 若null则忽略该条件
     * @return
     */
    private boolean matchLimiterRule(RequestLimiterMsgData msgData, Boolean async) {
        final List<LimiterRule> rules = new ArrayList<>();
        rules.addAll(this.cfg.getRequestPerIp());
        rules.addAll(this.cfg.getRequestPerCookie());
        rules.addAll(this.cfg.getResourcePerIp());
        rules.addAll(this.cfg.getResourcePerCookie());
        
        for (LimiterRule rule : rules) {
            if (rule.sleep == 0) { // sleep=0 不限制
                continue;
            }
            
            if (async != null) {
                if (rule.async != async) {
                    continue;
                }
            }

            if (rule.match(msgData.getResource(), msgData.getMethod())) {
                return true;
            }
        }
        
        return false;
    }

    private boolean checkRate(RequestLimiterMsgData msgData, String topic, List<LimiterRule> rules, boolean async) {
        for (LimiterRule rule : rules) {
            if (rule.sleep == 0) { // sleep=0 不限制
                continue;
            }
            if (rule.async != async) {
                continue;
            }

            if (rule.match(msgData.getResource(), msgData.getMethod())) {
                String key = bucketCounterKey(msgData.getTimestamp(), rule.duration, topic);
                final long num = this.bucket.increment(key, 1);
                // 设置有效期，避免内存长期占用内存
                // TODO incr原子性操作无法设置expire，存在极端情况, 且使用multi或lua脚本影响性能，后面可以每日定时任务，清除无 TTL 值的key
                if (num == 1) {
                    this.bucket.expire(key, rule.duration);
                }
                
                logger.trace("Checking limit, times: {}, rediskey: {}, rid: {}", num, key, msgData.getRid());

                if (num > rule.limit) {
                    logger.warn("exceeded limit rule: {}", rule);
                    
                    // 设置block标示
                    if (rule.sleep > 0) {
                        this.bucket.set(bucketBlockedKey(topic), "1", rule.sleep);
                    }
                    
                    // alarm
                    StringBuilder body = new StringBuilder()
                            .append("<gateway>").append(NetworkUtils.getLocalHostName())
                            .append("\n<rule> ").append(rule)
                            .append("\n<async> ").append(async)
                            .append("\n<topic> ").append(topic)
                            .append("\n<msg-data> ").append(msgData);
                    ErrorMsgData errorData = new ErrorMsgData();
                    errorData.setRid(msgData.getRid());
                    if (rule.sleep > 0) {
                        errorData.setSubject("Request Blocked (Limit Exceeded)");    
                    } else {
                        errorData.setSubject("Request Warning (Limit Exceeded)");    
                    }
                    errorData.setBody(body.toString());
                    alarmService.onError(errorData);
                    
                    return false;
                }
            }
        }
        
        return true;
    }

    private String bucketCounterKey(Long ts, long duration, String topic) {
        if (ts == null) {
            ts = System.currentTimeMillis();
        }

        final long factor = ts / duration;
        return "gw.bucket.counter::duration=" + duration + "," + factor + "," + topic;
    }

    private String bucketBlockedKey(String topic) {
        return "gw.bucket.blocked::"+topic;
    }

    protected String md5Hex(String s) {
        String md5Hex = DigestUtils.md5Hex(s);
        return md5Hex;
    }
}
