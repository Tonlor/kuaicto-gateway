package com.kuaicto.gateway.cache;

import com.kuaicto.gateway.matcher.PathMatcher;

public class CacheRule {
    final PathMatcher pathMatcher;
    private boolean withCookie;
    private long timeout;
    
    public CacheRule(PathMatcher pathMatcher, boolean withCookie, long timeout) {
        this.pathMatcher = pathMatcher;
        this.withCookie = withCookie;
        this.timeout = timeout;
    }
    
    public boolean match(String path, String method) {
        return this.pathMatcher.match(path, method);
    }

    public boolean isWithCookie() {
        return withCookie;
    }

    public long getTimeout() {
        return timeout;
    }
}
