package com.kuaicto.gateway.limiter;

import java.net.URI;

public class RequestLimiterMsgData {
    private URI uri;
    private String rid;
    private String ip;
    private String method;
    private String resource;
    private Long timestamp;
    private String cookie;
    public URI getUri() {
        return uri;
    }
    public void setUri(URI uri) {
        this.uri = uri;
    }
    public String getIp() {
        return ip;
    }
    public void setIp(String ip) {
        this.ip = ip;
    }
    public String getMethod() {
        return method;
    }
    public void setMethod(String method) {
        this.method = method;
    }
    public String getResource() {
        return resource;
    }
    public void setResource(String resource) {
        this.resource = resource;
    }
    public Long getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
    public String getCookie() {
        return cookie;
    }
    public void setCookie(String cookie) {
        this.cookie = cookie;
    }
    public String getRid() {
        return rid;
    }
    public void setRid(String rid) {
        this.rid = rid;
    }
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("RequestLimiterMsgData [uri=");
        builder.append(uri);
        builder.append(", rid=");
        builder.append(rid);
        builder.append(", ip=");
        builder.append(ip);
        builder.append(", method=");
        builder.append(method);
        builder.append(", resource=");
        builder.append(resource);
        builder.append(", timestamp=");
        builder.append(timestamp);
        builder.append(", cookie=");
        builder.append(cookie);
        builder.append("]");
        return builder.toString();
    }
}
