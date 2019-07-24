package com.kuaicto.gateway.matcher;

import org.apache.commons.lang.StringUtils;

import com.kuaicto.gateway.utils.PrefixChecker;


/**
 * 前缀路径匹配检查
 * @author martin
 */
public class PrefixPathMatcher implements PathMatcher {
    private String prefix;
    private PrefixChecker prefixChecker;
    private String method;

    public PrefixPathMatcher(String prefix, String method) {
        this.prefix = prefix;
        this.method = method;
        prefixChecker = new PrefixChecker(prefix);
    }
    
    public boolean match(String path, String method) {
        boolean methodMatched = StringUtils.isBlank(this.method) ? true : StringUtils.equalsIgnoreCase(this.method, method);
        return methodMatched 
                && prefixChecker.match(path);
    }

    public String getPrefix() {
        return prefix;
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("PrefixPathMatcher [prefix=");
        builder.append(prefix);
        builder.append(", method=");
        builder.append(method);
        builder.append("]");
        return builder.toString();
    }

    public static void main(String[] args) {
        PrefixPathMatcher matcher = new PrefixPathMatcher("/api/wechat/orderConfirm:/api/mall/orderConfirm:!/api/wechat/orderConfirm/amount:!/api/mall/orderConfirm/amount", "POST");
        System.out.println("-----true-----");
        System.out.println(matcher.match("/api/wechat/orderConfirm", "POST"));
        System.out.println(matcher.match("/api/mall/orderConfirm", "POST"));
        System.out.println(matcher.match("/api/wechat/orderConfirm/", "POST"));
        System.out.println(matcher.match("/api/mall/orderConfirm/", "POST"));

        System.out.println("-----false-----");
        
        System.out.println(matcher.match("/api/wechat/orderConfirm", "GET"));
        System.out.println(matcher.match("/api/mall/orderConfirm", "GET"));
        System.out.println(matcher.match("/api/wechat/orderConfirm/amount", "POST"));
        System.out.println(matcher.match("/api/mall/orderConfirm/amount", "POST"));
}
}
