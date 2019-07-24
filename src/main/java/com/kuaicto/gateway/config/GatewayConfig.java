package com.kuaicto.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Value("${gateway.appKey}")
    private String appKey;
    
    @Value("${gateway.appSecret}")
    private String appSecret;
    
    /**
     * auth token的cookie名, 默认为空字符串
     */
    @Value("${gateway.auth.cookieName:_MCH_AT}")
    private String cookieName;

    @Value("${gateway.error.http500ErrorVisible:true}")
    private boolean http500ErrorVisible;

    public String getCookieName() {
        return cookieName;
    }

    public void setCookieName(String cookieName) {
        this.cookieName = cookieName;
    }

    public boolean isHttp500ErrorVisible() {
        return http500ErrorVisible;
    }

    public void setHttp500ErrorVisible(boolean http500ErrorVisible) {
        this.http500ErrorVisible = http500ErrorVisible;
    }

    public String getAppKey() {
        return appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }
}
