package com.kuaicto.gateway.matcher;

import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;


/**
 * 正则路径匹配检查
 * @author martin
 */
public class RegexPathMatcher implements PathMatcher {

    private Pattern pattern;
    private String method;
    private String regex;

    public RegexPathMatcher(String regex, String method) {
        this.regex = regex;
        this.method = method;
        this.pattern = Pattern.compile(regex);
    }
    
    public boolean match(String path, String method) {
        boolean methodMatched = StringUtils.isBlank(this.method) ? true : StringUtils.equalsIgnoreCase(this.method, method);
        return methodMatched 
                && pattern.matcher(path).matches();
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("RegexPathMatcher [method=");
        builder.append(method);
        builder.append(", regex=");
        builder.append(regex);
        builder.append("]");
        return builder.toString();
    }

    public static void main(String[] args) {
        RegexPathMatcher matcher = new RegexPathMatcher("^/api/(wechat|mall)/orderConfirm(/)?$", "POST");
        System.out.println("-----true-----");
        System.out.println(matcher.match("/api/wechat/orderConfirm", "POST"));
        System.out.println(matcher.match("/api/mall/orderConfirm", "POST"));
        System.out.println(matcher.match("/api/wechat/orderConfirm/", "POST"));
        System.out.println(matcher.match("/api/mall/orderConfirm/", "POST"));

        System.out.println("-----false-----");
        
        System.out.println(matcher.match("/api/app/orderConfirm/", "POST"));
        System.out.println(matcher.match("/api/wechat/orderConfirm", "GET"));
        System.out.println(matcher.match("/api/mall/orderConfirm", "GET"));
        System.out.println(matcher.match("/api/wechat/orderConfirm/amount", "POST"));
        System.out.println(matcher.match("/api/mall/orderConfirm/amount", "POST"));
    }
}
