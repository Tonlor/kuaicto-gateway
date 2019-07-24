package com.kuaicto.gateway.matcher;


/**
 * 路径匹配检查
 * @author martin
 */
public interface PathMatcher {
    /**
     * 检查路径是否匹配
     */
    boolean match(String path, String method);
}
