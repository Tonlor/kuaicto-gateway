package com.kuaicto.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;

public class GwDefaultGatewayFilterFactory extends AbstractGatewayFilterFactory<GwDefaultGatewayFilterFactory.Config> {
    private static final Logger logger = LoggerFactory.getLogger(GwDefaultGatewayFilterFactory.class);

    public GwDefaultGatewayFilterFactory() {
        super(Config.class);
    }
    
    @Override
    public GatewayFilter apply(Config obj) {

        return (exchange, chain) -> {
            // TODO do something
            return chain.filter(exchange);
        };
    }
    
    public static class Config {
        
    }
}
