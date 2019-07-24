package com.kuaicto.gateway.filter;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

import com.kuaicto.gateway.limiter.RequestLimiterCenter;
import com.kuaicto.gateway.limiter.RequestLimiterMsgData;
import com.kuaicto.gateway.utils.HttpUtils;

/**
 * 基于IP的限流
 */
public class GwRequestRateLimiterGlobalFilter implements GlobalFilter, Ordered {
    protected static final Logger logger = LoggerFactory.getLogger(GwRequestRateLimiterGlobalFilter.class);

    @Autowired
    private RequestLimiterCenter limiterCenter;
    
    public GwRequestRateLimiterGlobalFilter() {
    }
    
    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 限流判断
        boolean checkRate = true;
        try {
            checkRate = checkRate(exchange, chain);
        } catch (Exception e) {
            logger.error("CheckRate error", e);
        }

        // 继续处理请求
        if (checkRate) {
            return chain.filter(exchange);
        } else {
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return exchange.getResponse().setComplete();
        }
    }
    
    private boolean checkRate(ServerWebExchange exchange, GatewayFilterChain chain) {
        final long start = System.currentTimeMillis();
        final String rid = exchange.getRequest().getHeaders().getFirst("X-SGW-REQUEST-ID");

        final String ip = getRemoteAddr(exchange);
        final ServerHttpRequest request = exchange.getRequest();
        final String resource = request.getPath().value();
        final String cookieValue = this.getCookieValue(exchange, this.limiterCenter.getRequestLimiterConfig().getCookieName());

        final RequestLimiterMsgData msgData = new RequestLimiterMsgData();
        msgData.setCookie(cookieValue);
        msgData.setRid(rid);
        msgData.setIp(ip);
        msgData.setMethod(request.getMethodValue());
        msgData.setResource(resource);
        msgData.setTimestamp(System.currentTimeMillis());
        msgData.setUri(request.getURI());
        
        final boolean checked = limiterCenter.onRequestReceived(msgData);
        
        
        final long end = System.currentTimeMillis();
        logger.debug("[rid: {}] CheckRate Time-Taken: {}ms", rid, (end-start));
        
        return checked;
    }

    protected String getRemoteAddr(ServerWebExchange exchange) {
//        String ip = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        String ip = HttpUtils.getFirstHeader(exchange.getRequest().getHeaders(), "X-Forwarded-For");
        if (StringUtils.isBlank(ip)) {
            ip = exchange.getRequest().getRemoteAddress().getHostString();
        }
        return ip;
    }

    protected String getCookieValue(ServerWebExchange exchange, String cookieName) {
        if (StringUtils.isBlank(cookieName)) {
            return null;
        }
        HttpCookie httpCookie = exchange.getRequest().getCookies().getFirst(cookieName);
        if (httpCookie != null) {
            return httpCookie.getValue();
        }
        return null;
    }

    protected String md5Hex(String s) {
        String md5Hex = DigestUtils.md5Hex(s);
        return md5Hex;
    }
}
