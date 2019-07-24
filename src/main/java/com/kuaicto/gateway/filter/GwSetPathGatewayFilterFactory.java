package com.kuaicto.gateway.filter;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.addOriginalRequestUrl;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.util.UriTemplate;
import org.springframework.web.util.pattern.PathPattern.PathMatchInfo;

import com.kuaicto.gateway.utils.URIBuilder;

public class GwSetPathGatewayFilterFactory extends AbstractGatewayFilterFactory<GwSetPathGatewayFilterFactory.Config> {
    
    public GwSetPathGatewayFilterFactory() {
        super(Config.class);
    }
    
    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("template");
    }

	@Override
	public GatewayFilter apply(Config config) {
		UriTemplate uriTemplate = new UriTemplate(config.template);

		return (exchange, chain) -> {
			PathMatchInfo variables = exchange.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE);
			ServerHttpRequest req = exchange.getRequest();
			addOriginalRequestUrl(exchange, req.getURI());
			Map<String, String> uriVariables;

			if (variables != null) {
				uriVariables = variables.getUriVariables();
			} else {
				uriVariables = Collections.emptyMap();
			}

			URI uri = uriTemplate.expand(uriVariables);

			exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, uri);
			
			URI newURI = URIBuilder.MergeURI(req.getURI(), uri);
			ServerHttpRequest request = req.mutate().uri(newURI).build();

			return chain.filter(exchange.mutate().request(request).build());
		};
	}
	
	public static class Config {
        private String template;

        public String getTemplate() {
            return template;
        }

        public void setTemplate(String template) {
            this.template = template;
        }
    }
}
