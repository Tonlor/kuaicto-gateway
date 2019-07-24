package com.kuaicto.gateway.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;

@Configuration
public class HandlerMappingConfig {
    
    @Bean
    public SimpleUrlHandlerMapping simpleUrlHandlerMapping() {
        SimpleUrlHandlerMapping simpleUrlHandlerMapping = new SimpleUrlHandlerMapping();
        simpleUrlHandlerMapping.setOrder(Integer.MAX_VALUE - 2);
         
        Map<String, Object> urlMap = new HashMap<>();
//        urlMap.put("/gateway/requestLimiter", requestLimiterController());
        simpleUrlHandlerMapping.setUrlMap(urlMap);
         
        return simpleUrlHandlerMapping;
    }
 
//    @Bean
//    public RequestLimiterController requestLimiterController() {
//        return new RequestLimiterController();
//    }
}
