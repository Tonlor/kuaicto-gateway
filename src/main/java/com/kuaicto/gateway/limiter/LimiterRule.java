package com.kuaicto.gateway.limiter;

import com.kuaicto.gateway.matcher.PathMatcher;

/**
 * 限流规则
 * @author martin
 */
public class LimiterRule {
    /**
     * 是否异步检查
     */
    public final boolean async;
    /**
     * 屏蔽时间，单位毫秒（正数：屏蔽， 零：忽略， 负数：只警告，不屏蔽）
     */
    public final long sleep;
    /**
     * 限制次数
     */
    public final long limit;
    /**
     * 周期，单位毫秒
     */
    public final long duration;
    
    final PathMatcher pathMatcher;
    
    /**
     * @param sleepMillis 屏蔽时间，单位毫秒（正数：屏蔽， 零：忽略， 负数：只警告，不屏蔽）
     * @param duration 周期，单位毫秒
     * @param limit 限制次数 
     */
    public LimiterRule(long sleepMillis, long duration, long limit, PathMatcher pathMatcher, boolean async) {
        if (limit < 0 || duration < 0) {
            throw new IllegalArgumentException("MUST: limit > 0, duration > 0");
        }
        
        this.sleep = sleepMillis;
        this.duration = duration;
        this.limit = limit;
        this.pathMatcher = pathMatcher;
        this.async = async;
    }
    
    public boolean match(String path, String method) {
        return this.pathMatcher.match(path, method);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("LimiterRule [async=");
        builder.append(async);
        builder.append(", sleep=");
        builder.append(sleep);
        builder.append(", limit=");
        builder.append(limit);
        builder.append(", duration=");
        builder.append(duration);
        builder.append(", pathMatcher=");
        builder.append(pathMatcher);
        builder.append("]");
        return builder.toString();
    }
}
