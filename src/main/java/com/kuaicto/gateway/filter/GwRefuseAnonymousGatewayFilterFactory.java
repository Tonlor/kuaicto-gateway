package com.kuaicto.gateway.filter;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

import com.kuaicto.gateway.Constants;
import com.kuaicto.gateway.utils.PrefixChecker;

public class GwRefuseAnonymousGatewayFilterFactory extends AbstractGatewayFilterFactory<GwRefuseAnonymousGatewayFilterFactory.Config> {
	private static final Logger logger = LoggerFactory.getLogger(GwRefuseAnonymousGatewayFilterFactory.class);

	public GwRefuseAnonymousGatewayFilterFactory() {
	    super(Config.class);
	}
	
    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("prefix");
    }

	public GatewayFilter apply(Config config) {
		final PrefixChecker prefixChecker = new PrefixChecker(config.prefix);
		
		return new GatewayFilter() {

			public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
				
				String path = exchange.getRequest().getPath().value();
				List<String> list = exchange.getRequest().getHeaders().get(Constants.X_SGW_SESSION_USER_ENCODED);
				// 拒绝匿名访问
				boolean anonymous = list == null || list.isEmpty();
				if (anonymous && prefixChecker.match(path)) {
					logger.warn("UNAUTHORIZED: Anonymous request forbiden: {}", path);
					exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
					return exchange.getResponse().setComplete();
				}
				
				return chain.filter(exchange);
			}
		};
	}
    public static class Config {
        private String prefix;
        public String getPrefix() {
            return prefix;
        }
        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }
    }

}
