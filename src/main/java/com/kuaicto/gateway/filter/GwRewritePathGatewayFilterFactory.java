package com.kuaicto.gateway.filter;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.addOriginalRequestUrl;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;

import com.kuaicto.gateway.utils.URIBuilder;

/**
 * @author Martin Han
 */
public class GwRewritePathGatewayFilterFactory extends AbstractGatewayFilterFactory<GwRewritePathGatewayFilterFactory.Config> {

	public static final String REGEXP_KEY = "regexp";
	public static final String REPLACEMENT_KEY = "replacement";

	public GwRewritePathGatewayFilterFactory() {
	    super(Config.class);
	}
	
    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList(REGEXP_KEY, REPLACEMENT_KEY);
    }

	@Override
	public GatewayFilter apply(Config config) {
		final String regex = config.regexp;
		String replacement = config.replacement.replace("$\\", "$");

		return (exchange, chain) -> {
			ServerHttpRequest req = exchange.getRequest();
			addOriginalRequestUrl(exchange, req.getURI());
			String path = req.getURI().getRawPath();
			String newPath = path.replaceAll(regex, replacement);

			URI newURI = URIBuilder.MergeURI(req.getURI(), newPath);
			
			ServerHttpRequest request = req.mutate()
					.uri(newURI)
					.build();

			exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, request.getURI());

			return chain.filter(exchange.mutate().request(request).build());
		};
	}
	
	public static class Config {
        private String regexp;
        private String replacement;

        public String getRegexp() {
            return regexp;
        }

        public Config setRegexp(String regexp) {
            this.regexp = regexp;
            return this;
        }

        public String getReplacement() {
            return replacement;
        }

        public Config setReplacement(String replacement) {
            this.replacement = replacement;
            return this;
        }
    }

}
