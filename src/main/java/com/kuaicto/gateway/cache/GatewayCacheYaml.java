package com.kuaicto.gateway.cache;

import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.kuaicto.gateway.matcher.PathMatcher;
import com.kuaicto.gateway.matcher.PrefixPathMatcher;
import com.kuaicto.gateway.matcher.RegexPathMatcher;
import com.kuaicto.gateway.utils.YamlFileLoader;

public class GatewayCacheYaml {
    public static class Item {
        private String method;
        private String path;
        private boolean withCookie = false;
        private long timeout = 30 * 1000; // 默认30s

        public CacheRule toCacheRule() {
            CacheRule rule = new CacheRule(getMatcher(), this.withCookie, this.timeout);
            return rule;
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

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public boolean isWithCookie() {
            return withCookie;
        }

        public void setWithCookie(boolean withCookie) {
            this.withCookie = withCookie;
        }

        public long getTimeout() {
            return timeout;
        }

        public void setTimeout(long timeout) {
            this.timeout = timeout;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("Item [method=");
            builder.append(method);
            builder.append(", path=");
            builder.append(path);
            builder.append(", withCookie=");
            builder.append(withCookie);
            builder.append(", timeout=");
            builder.append(timeout);
            builder.append("]");
            return builder.toString();
        }
    }
    
    private static GatewayCacheYaml instance;
    
    private List<Item> items;
    private String cookieName;
    private boolean enabled = false;
    
    public GatewayCacheYaml() {
    }
    
    public synchronized static GatewayCacheYaml load() throws Exception {
        if (instance == null) {
            instance = YamlFileLoader.loadAs("gateway-cache.yml", GatewayCacheYaml.class);
        }
        return instance;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public String getCookieName() {
        return cookieName;
    }

    public void setCookieName(String cookieName) {
        this.cookieName = cookieName;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("GatewayCacheYaml [items=");
        builder.append(items);
        builder.append(", cookieName=");
        builder.append(cookieName);
        builder.append(", enabled=");
        builder.append(enabled);
        builder.append("]");
        return builder.toString();
    }

}
