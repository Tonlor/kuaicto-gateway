package com.kuaicto.gateway.filter;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

import com.kuaicto.gateway.utils.PrefixChecker;
import com.kuaicto.gateway.utils.RestClient;

public class GwAuthGatewayFilterFactory extends AbstractGatewayFilterFactory<GwAuthGatewayFilterFactory.Config> {
	private static final Logger logger = LoggerFactory.getLogger(GwAuthGatewayFilterFactory.class);
	
	@Autowired
	RestClient restClient;
	
	public GwAuthGatewayFilterFactory() {
	    super(Config.class);
	}
	
	@Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("url");
    }

	public GatewayFilter apply(Config config) {
		logger.trace("Applying filter, config: {}", config);
		final String url = config.url;
		final PrefixChecker prefixChecker = new PrefixChecker(config.url);
		
		return new GatewayFilter() {

			public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
				
				String path = exchange.getRequest().getPath().value();
				logger.debug("Checking permission, path: {}", path);
				
				boolean matched = prefixChecker.match(path);
				// 若不匹配则跳过授权验证
				if (! matched) {
					logger.debug("Skip Checking permission, path: {}", path);
					return chain.filter(exchange);
				}

				String body = restClient.requestQuietly(exchange, url + "?url=" + path + "&method=" + exchange.getRequest().getMethodValue());

				// 授权成功
				if ("true".equalsIgnoreCase(body)) {
					logger.info("Request continue: {}", path);
					return chain.filter(exchange);
				}
				else {
					// 授权失败
					logger.warn("Request forbiden: {}", path);
					exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
					return exchange.getResponse().setComplete();
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
