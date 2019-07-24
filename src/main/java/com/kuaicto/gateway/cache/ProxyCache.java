package com.kuaicto.gateway.cache;

import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

import com.kuaicto.gateway.utils.HttpUtils;

public class ProxyCache {
//    private static final Logger logger = LoggerFactory.getLogger(ProxyCache.class);
    
    private StringRedisTemplate redisTemplate;
    private GatewayCacheConfig gatewayCacheConfig;
    
    public ProxyCache(StringRedisTemplate redisTemplate) throws Exception {
        this.redisTemplate = redisTemplate;
        this.gatewayCacheConfig = GatewayCacheConfig.from(GatewayCacheYaml.load());
    }
    
    private CacheRule getCacheRule(ServerHttpRequest request, String cookie) {
        if (!gatewayCacheConfig.isEnabled()) {
            return null;
        }
        for (CacheRule rule : gatewayCacheConfig.getCacheRules()) {
            if (rule.match(request.getPath().value(), request.getMethodValue())) {
                if (rule.isWithCookie()) {
                    if (StringUtils.isNotBlank(cookie)) {
                        return rule;
                    }
                    continue;
                }
                return rule;
            }
        }
        
        return null;
    }
    
    public String getCacheForRequest(ServerHttpRequest request) {
        final String cookie = this.getCookieValue(request, this.gatewayCacheConfig.getCookieName());
        CacheRule cacheRule = getCacheRule(request, cookie);
        if (cacheRule == null) {
            return null;
        }
        
        if (cacheRule.isWithCookie()) {
            return redisTemplate.opsForValue().get(key(request, cookie));
        }
        
        return redisTemplate.opsForValue().get(key(request, null));
    }
    
    public boolean putCacheForRequest(ServerHttpRequest request, String dataCache) {
        final String cookie = this.getCookieValue(request, this.gatewayCacheConfig.getCookieName());
        CacheRule cacheRule = getCacheRule(request, cookie);
        if (cacheRule == null) {
            return false;
        }
        
        if (cacheRule.isWithCookie()) {
            redisTemplate.opsForValue().set(key(request, cookie), dataCache, cacheRule.getTimeout(), TimeUnit.MILLISECONDS);
        } else {
            redisTemplate.opsForValue().set(key(request, null), dataCache, cacheRule.getTimeout(), TimeUnit.MILLISECONDS);
        }

        return true;
    }

    private String key(ServerHttpRequest request, String cookie) {
        StringBuilder builder = new StringBuilder("gw.cache::");
        builder.append(request.getURI().toString());
        
        if (StringUtils.isNotBlank(cookie)) {
            builder.append("::");
            builder.append(cookie);
        }
        
        return builder.toString();
    }
    
    protected String getRemoteAddr(ServerWebExchange exchange) {
//        String ip = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        String ip = HttpUtils.getFirstHeader(exchange.getRequest().getHeaders(), "X-Forwarded-For");
        if (StringUtils.isBlank(ip)) {
            ip = exchange.getRequest().getRemoteAddress().getHostString();
        }
        return ip;
    }

    protected String getCookieValue(ServerHttpRequest request, String cookieName) {
        if (StringUtils.isBlank(cookieName)) {
            return null;
        }
        HttpCookie httpCookie = request.getCookies().getFirst(cookieName);
        if (httpCookie != null) {
            return httpCookie.getValue();
        }
        return null;
    }
}
