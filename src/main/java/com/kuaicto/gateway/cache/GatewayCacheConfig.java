package com.kuaicto.gateway.cache;

import java.util.List;
import java.util.stream.Collectors;

public class GatewayCacheConfig {
    private List<CacheRule> cacheRules;
    private String cookieName;
    private boolean enabled = false;
    
    public GatewayCacheConfig() {
    }
    
    public boolean isEnabled() {
        return enabled;
    }

    public List<CacheRule> getCacheRules() {
        return cacheRules;
    }

    public String getCookieName() {
        return cookieName;
    }

    public static GatewayCacheConfig from(GatewayCacheYaml yaml) {
        GatewayCacheConfig config = new GatewayCacheConfig();
        config.cookieName = yaml.getCookieName();
        config.enabled = yaml.isEnabled();
        config.cacheRules = yaml.getItems().stream().map(GatewayCacheYaml.Item::toCacheRule).collect(Collectors.toList());
        
        return config;
    }
}
