package com.kuaicto.gateway.alarm;

public class ErrorMsgData {
    private String rid;
    private String subject;
    private String body;
    public String getRid() {
        return rid;
    }
    public void setRid(String rid) {
        this.rid = rid;
    }
    public String getBody() {
        return body;
    }
    public void setBody(String body) {
        this.body = body;
    }
    public String getSubject() {
        return subject;
    }
    public void setSubject(String subject) {
        this.subject = subject;
    }
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ErrorMsgData [rid=");
        builder.append(rid);
        builder.append(", subject=");
        builder.append(subject);
        builder.append(", body=");
        builder.append(body);
        builder.append("]");
        return builder.toString();
    }
}
