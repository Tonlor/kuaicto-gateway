package com.kuaicto.gateway.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

import com.kuaicto.gateway.alarm.ErrorAlarmService;
import com.kuaicto.gateway.config.GatewayConfig;
import com.kuaicto.gateway.utils.HttpUtils;

public class GwDefaultGlobalFilter implements GlobalFilter, Ordered {
    private static final String X_TOKEN = "X-Token";

    private static final Logger logger = LoggerFactory.getLogger(GwDefaultGlobalFilter.class);

    private GatewayConfig gatewayConfig;
    private ErrorAlarmService errorAlarm;

    public GwDefaultGlobalFilter(GatewayConfig gatewayConfig, ErrorAlarmService errorAlarm) {
        this.gatewayConfig = gatewayConfig;
        this.errorAlarm = errorAlarm;
    }
    
    @Override
    public int getOrder() {
        return -1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // use x-token header if the related cookie missing
        String cookieLine = buildNewCookieLine(exchange);

        // PUT REQUEST-ID
        String rid = UUID.randomUUID().toString();
        HttpCookie httpCookie = getAuthCookie(exchange);
        String cid = getCookieId(httpCookie);
        ServerHttpRequest request = exchange.getRequest().mutate().headers(item -> {
                    item.set("Cookie", cookieLine);
                })
                .header("X-SGW-REQUEST-ID", rid)
                .header("X-SGW-COOKIE-ID", StringUtils.trimToEmpty(cid))
                .build();
        if (httpCookie != null) {
            exchange.getAttributes().put("X-SGW-COOKIE", httpCookie);
        }
        
        // PUT response: rid
        exchange.getResponse().getHeaders().add("X-SGW-REQUEST-ID", rid);
        
        // JUST LOGGING
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.addAll(request.getHeaders());
        HttpEntity<String> requestEntity = new HttpEntity<String>(requestHeaders);
        logger.debug("<Request-Meta>: {} {}, headers: {}", request.getMethodValue(), request.getURI(), requestEntity.toString());
        
        // 继续处理
        try {
            return chain.filter(exchange.mutate().request(request).build());
        } catch (Exception e) {
            if (gatewayConfig.isHttp500ErrorVisible()) {
                throw e;
            }
            logger.error("[rid: {}] {}", rid, e.getMessage(), e);
            exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return exchange.getResponse().setComplete();
        } finally {
//            long timeTaken = System.currentTimeMillis() - start;
//            logger.debug("[rid: {}] <<Time-Taken>> : {}", rid, timeTaken);
        }

    }
    
    /**
     * 使用Header(X-Token)覆盖相关Cookie
     * @param exchange
     * @return 
     */
    private String buildNewCookieLine(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        List<HttpCookie> all = new ArrayList<>();
        List<HttpCookie> list = request.getCookies().get(this.gatewayConfig.getCookieName());
        if (CollectionUtils.isEmpty(list)) {
            String token = StringUtils.trim(HttpUtils.getFirstHeader(request.getHeaders(), X_TOKEN));
            if (StringUtils.isNotBlank(token)) {
                HttpCookie httpCookie = new HttpCookie(this.gatewayConfig.getCookieName(), token);
                all.add(httpCookie);
            }
        }
        
        request.getCookies().values().forEach(items -> {
            all.addAll(items);
        });
        StringBuilder sb = new StringBuilder();
        all.forEach(cookie -> {
            sb.append(cookie.getName()).append("=").append(cookie.getValue()).append("; ");
        });
        
        return sb.toString();
    }
    
    private HttpCookie getAuthCookie(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        HttpCookie httpCookie = request.getCookies().getFirst(this.gatewayConfig.getCookieName());
        if (httpCookie == null) {
            String token = HttpUtils.getFirstHeader(request.getHeaders(), X_TOKEN);
            if (StringUtils.isNotBlank(token)) {
                httpCookie = new HttpCookie(this.gatewayConfig.getCookieName(), token);
            }
        }
        return httpCookie;
    }

    protected String getCookieId(HttpCookie httpCookie) {
        if (httpCookie != null) {
            String md5Hex = DigestUtils.md5Hex(httpCookie.getValue());
            return md5Hex;
        }
        
        return null;
    }
}
