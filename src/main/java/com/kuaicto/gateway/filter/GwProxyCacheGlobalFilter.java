package com.kuaicto.gateway.filter;

import java.io.ByteArrayOutputStream;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import com.kuaicto.gateway.cache.ProxyCache;
import com.kuaicto.gateway.utils.HttpUtils;

/**
 * 基于IP的限流
 */
public class GwProxyCacheGlobalFilter implements GlobalFilter, Ordered {
    protected static final Logger logger = LoggerFactory.getLogger(GwProxyCacheGlobalFilter.class);

    ProxyCache proxyCache;
    
    public GwProxyCacheGlobalFilter(StringRedisTemplate redisTemplate) throws Exception {
        this.proxyCache = new ProxyCache(redisTemplate);
    }
    
    @Override
    public int getOrder() {
        return -2;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        DataBufferFactory dataBufferFactory = response.bufferFactory();

        /////////////////////////////CACHE////////////////////////////////////////////////
        String cached = this.proxyCache.getCacheForRequest(request);
        if (cached != null) {
            DataBuffer dataBuffer = dataBufferFactory.wrap(cached.getBytes());
            return response.writeWith(Flux.just(dataBuffer));
        }
        
        /////////////////////////////////////////////////////////////////////////////

        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(response) {
        @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                if (body instanceof Flux) {
                    Flux<? extends DataBuffer> flux = (Flux<? extends DataBuffer>) body;

                    return super
                            .writeWith(flux
                                    .buffer()
                                    .map(dataBuffers -> {
                                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                                        dataBuffers.forEach(i -> {
                                            byte[] array = new byte[i.readableByteCount()];
                                            i.read(array);
                                            try {
                                                outputStream.write(array);
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                        });
                                        proxyCache.putCacheForRequest(request, new String(outputStream.toByteArray()));
                                        return dataBufferFactory.wrap(outputStream.toByteArray());
                                    }));
                }
                return super.writeWith(body);
            }
        };

        return chain.filter(exchange.mutate().response(decoratedResponse).build());
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
