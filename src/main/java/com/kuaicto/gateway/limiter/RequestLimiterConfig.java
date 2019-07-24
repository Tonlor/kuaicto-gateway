package com.kuaicto.gateway.limiter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kuaicto.gateway.limiter.RequestLimiterYaml.RuleItem;

public class RequestLimiterConfig {
    protected static final Logger logger = LoggerFactory.getLogger(RequestLimiterConfig.class);
    
    private List<LimiterRule> requestPerIp;
    private List<LimiterRule> requestPerCookie;
    private List<LimiterRule> resourcePerIp;
    private List<LimiterRule> resourcePerCookie;
    private String cookieName;
    private boolean enabled;
    private Set<String> whitelist;
    private Set<String> blacklist;
    
    public RequestLimiterConfig() {
    }
    
    public static RequestLimiterConfig load() throws Exception {
        RequestLimiterYaml yaml = RequestLimiterYaml.loadFromFile();
        RequestLimiterConfig cfg = new RequestLimiterConfig();
        
        cfg.blacklist = yaml.getBlacklist();
        if (cfg.blacklist == null) {
            cfg.blacklist = Collections.emptySet();
        }
        
        cfg.whitelist = yaml.getWhitelist();
        if (cfg.whitelist == null) {
            cfg.whitelist = Collections.emptySet();
        }

        cfg.cookieName = yaml.getCookieName();
        cfg.enabled = yaml.isEnabled();
        
        cfg.requestPerIp = toLimiterRules(yaml.getRequestPerIp());
        cfg.requestPerCookie = toLimiterRules(yaml.getRequestPerCookie());
        cfg.resourcePerIp = toLimiterRules(yaml.getResourcePerIp());
        cfg.resourcePerCookie = toLimiterRules(yaml.getResourcePerCookie());
        
        return cfg;
    }
    
    private static List<LimiterRule> toLimiterRules(List<RuleItem> items) {
        if (items == null) {
            return Collections.emptyList();
        }
        ArrayList<LimiterRule> list = new ArrayList<LimiterRule>();
        items.forEach(item -> {
            list.addAll(item.toLimiterRules());
        });
        return list;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<LimiterRule> getRequestPerIp() {
        return requestPerIp;
    }

    public List<LimiterRule> getRequestPerCookie() {
        return requestPerCookie;
    }

    public List<LimiterRule> getResourcePerIp() {
        return resourcePerIp;
    }

    public List<LimiterRule> getResourcePerCookie() {
        return resourcePerCookie;
    }

    public String getCookieName() {
        return cookieName;
    }

    public Set<String> getWhitelist() {
        return whitelist;
    }

    public Set<String> getBlacklist() {
        return blacklist;
    }
}
