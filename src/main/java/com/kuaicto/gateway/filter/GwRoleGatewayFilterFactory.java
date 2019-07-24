package com.kuaicto.gateway.filter;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

import com.kuaicto.gateway.utils.RestClient;

public class GwRoleGatewayFilterFactory extends AbstractGatewayFilterFactory<GwRoleGatewayFilterFactory.Config> {
	private static final Logger logger = LoggerFactory.getLogger(GwRoleGatewayFilterFactory.class);
	
	@Autowired
	RestClient restClient;
	
	public GwRoleGatewayFilterFactory() {
	    super(Config.class);
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
				
				String path = exchange.getRequest().getPath().value();
				logger.debug("Requesting active role for path: {}", path);

				String body = restClient.requestQuietly(exchange, url);

				// 授权成功
				if (StringUtils.isNotBlank(body)) {
					// 设置header
					logger.info("Set-Header: X-SGW-SESSION-ROLES={}", body);
					ServerHttpRequest request = exchange.getRequest().mutate().header("X-SGW-SESSION-ROLES", body).build();
					return chain.filter(exchange.mutate().request(request).build());
				}
				else {
					// 授权失败
					logger.warn("Roles not found. Skipping Set-Header: X-SGW-SESSION-ROLES");
					return chain.filter(exchange);
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
