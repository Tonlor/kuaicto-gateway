package com.kuaicto.gateway.filter;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

import com.kuaicto.gateway.Constants;
import com.kuaicto.gateway.cache.ProfileCache;
import com.kuaicto.gateway.config.GatewayConfig;
import com.kuaicto.gateway.utils.RestClient;

public class GwProfileGatewayFilterFactory extends AbstractGatewayFilterFactory<GwProfileGatewayFilterFactory.Config> {

    private static final Logger logger = LoggerFactory.getLogger(GwProfileGatewayFilterFactory.class);

    /**
     * 缓存过期时间
     */
	private static final long expires = 5 * 60 * 1000;

	@Autowired
	RestClient restClient;
	
	@Autowired
    GatewayConfig gatewayConfig;

    private final ProfileCache profileCache;
	
	public GwProfileGatewayFilterFactory(StringRedisTemplate redisTemplate) {
	    super(Config.class);
	    profileCache = new ProfileCache(redisTemplate);
	}
	
    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("url");
    }

	public GatewayFilter apply(Config config) {
		logger.trace("Applying filter, config: {}", config);
		final String url = config.url;
		
		return new GatewayFilter() {

			public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		        final String rid = exchange.getRequest().getHeaders().getFirst("X-SGW-REQUEST-ID");
		        final String method = exchange.getRequest().getMethodValue();
				final String path = exchange.getRequest().getPath().value();
				logger.debug("Get profile for requesting: {}", path);
				
				String userInfo = null;

				// cache:start 
				HttpCookie httpCookie = exchange.getAttribute("X-SGW-COOKIE");
				if (httpCookie != null) {
				    final String cookie = httpCookie.getValue();
				    logger.debug("Cookie: {}={}", httpCookie.getName(), httpCookie.getValue());
				    if (StringUtils.isNotBlank(cookie)) {
				        final String key = cookie;
				        userInfo = profileCache.get(key);
				        if (userInfo == null) {
                            logger.debug("CACHE SET");
                            userInfo = restClient.requestQuietly(exchange, url);
                            if (userInfo != null) {
                                profileCache.set(key, userInfo, expires);
                            }
				        } else {
				            logger.debug("CACHE HIT");
				        }
				    }
				} else {
				    logger.warn("Not found cookie");
				}
                // cache:end 
				
				if (StringUtils.isEmpty(userInfo)) {
                    logger.debug("Profile: Anonymous > {} {} > {}", method, path, rid);
					return chain.filter(exchange);
				} else {
					logger.debug("Profile: {} > {} {} > {}", userInfo, method, path, rid);
					// 设置header
					
					// 中文乱码问题
					String encode;
					try {
						encode = URLEncoder.encode(userInfo, "UTF-8");
					} catch (UnsupportedEncodingException e) {
						throw new RuntimeException(e);
					}
					
					// 使用2个header为了兼容旧版本
					ServerHttpRequest request = exchange.getRequest().mutate()
							.header(Constants.X_SGW_SESSION_USER, userInfo)
							.header(Constants.X_SGW_SESSION_USER_ENCODED, encode)
							.build();
					
					return chain.filter(exchange.mutate().request(request).build());
				}
			}
		};
	}
	
	public static class Config {
	    private String url;
        public String getUrl() {
            return url;
        }
        public void setUrl(String url) {
            this.url = url;
        }
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("Config [url=");
            builder.append(url);
            builder.append("]");
            return builder.toString();
        }
	}
}
