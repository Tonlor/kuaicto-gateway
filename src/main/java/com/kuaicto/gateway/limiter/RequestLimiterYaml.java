package com.kuaicto.gateway.limiter;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kuaicto.gateway.matcher.PathMatcher;
import com.kuaicto.gateway.matcher.PrefixPathMatcher;
import com.kuaicto.gateway.matcher.RegexPathMatcher;
import com.kuaicto.gateway.utils.YamlFileLoader;

public class RequestLimiterYaml {
    public static class RuleItem {
        private boolean async = true;
        private Long sleep;
        private String method;
        private String path;
        private String rules;
        public boolean isAsync() {
            return async;
        }
        public void setAsync(boolean async) {
            this.async = async;
        }
        public String getMethod() {
            return method;
        }
        public void setMethod(String method) {
            this.method = method;
        }
        public Long getSleep() {
            return sleep;
        }
        public void setSleep(Long sleep) {
            this.sleep = sleep;
        }
        public String getPath() {
            return path;
        }
        public void setPath(String path) {
            this.path = path;
        }
        public String getRules() {
            return rules;
        }
        public void setRules(String rules) {
            this.rules = rules;
        }

        public List<LimiterRule> toLimiterRules() {
            List<LimiterRule> rules = new ArrayList<LimiterRule>();
            
            String[] groups = StringUtils.split(this.rules, ";");
            for (String group : groups) {
                String[] items = group.split("/");
                long limit = Long.parseLong(items[0]);
                long duration = Long.parseLong(items[1]);
                LimiterRule rule = new LimiterRule(this.sleep * 1000, duration * 1000, limit, getMatcher(), this.async);
                rules.add(rule);
            }
            return rules;
            
        }
        private PathMatcher getMatcher() {
            if (StringUtils.startsWithIgnoreCase(this.path, "prefix=")) {
                return new PrefixPathMatcher(StringUtils.substring(this.path, "prefix=".length()), this.method);
            }
            if (StringUtils.startsWithIgnoreCase(this.path, "regex=")) {
                return new RegexPathMatcher(StringUtils.substring(this.path, "regex=".length()), this.method);
            }
            
            return new PrefixPathMatcher(this.path, this.method);
        }
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("RuleItem [sleep=");
            builder.append(sleep);
            builder.append(", path=");
            builder.append(path);
            builder.append(", rules=");
            builder.append(rules);
            builder.append("]");
            return builder.toString();
        }
    }
    
    protected static final Logger logger = LoggerFactory.getLogger(RequestLimiterYaml.class);
    
    private List<RuleItem> requestPerIp;
    private List<RuleItem> requestPerCookie;
    private List<RuleItem> resourcePerIp;
    private List<RuleItem> resourcePerCookie;
    private String cookieName;
    private boolean enabled = false;
    private Set<String> whitelist;
    private Set<String> blacklist;
    
    public RequestLimiterYaml() {
    }
    
    public static RequestLimiterYaml loadFromFile() throws FileNotFoundException {
        return YamlFileLoader.loadAs("request-limiter.yml", RequestLimiterYaml.class);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<RuleItem> getRequestPerIp() {
        return requestPerIp;
    }

    public List<RuleItem> getRequestPerCookie() {
        return requestPerCookie;
    }

    public List<RuleItem> getResourcePerIp() {
        return resourcePerIp;
    }

    public List<RuleItem> getResourcePerCookie() {
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

    public void setRequestPerIp(List<RuleItem> requestPerIp) {
        this.requestPerIp = requestPerIp;
    }

    public void setRequestPerCookie(List<RuleItem> requestPerCookie) {
        this.requestPerCookie = requestPerCookie;
    }

    public void setResourcePerIp(List<RuleItem> resourcePerIp) {
        this.resourcePerIp = resourcePerIp;
    }

    public void setResourcePerCookie(List<RuleItem> resourcePerCookie) {
        this.resourcePerCookie = resourcePerCookie;
    }

    public void setCookieName(String cookieName) {
        this.cookieName = cookieName;
    }

    public void setWhitelist(Set<String> whitelist) {
        this.whitelist = whitelist;
    }

    public void setBlacklist(Set<String> blacklist) {
        this.blacklist = blacklist;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("RequestLimiterYaml [requestPerIp=");
        builder.append(requestPerIp);
        builder.append(", requestPerCookie=");
        builder.append(requestPerCookie);
        builder.append(", resourcePerIp=");
        builder.append(resourcePerIp);
        builder.append(", resourcePerCookie=");
        builder.append(resourcePerCookie);
        builder.append(", cookieName=");
        builder.append(cookieName);
        builder.append(", whitelist=");
        builder.append(whitelist);
        builder.append(", blacklist=");
        builder.append(blacklist);
        builder.append("]");
        return builder.toString();
    }

}
