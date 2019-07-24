package com.kuaicto.gateway.alarm;

import java.util.Collections;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kuaicto.gateway.matcher.PathMatcher;
import com.kuaicto.gateway.matcher.PrefixPathMatcher;
import com.kuaicto.gateway.matcher.RegexPathMatcher;
import com.kuaicto.gateway.utils.YamlFileLoader;

public class AlarmConfigYaml {
    public static class Item {
        private String method;
        private String path;
        /** 默认500ms */
        private long time = 500; 

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
        public long getTime() {
            return time;
        }
        public void setTime(long time) {
            this.time = time;
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
            builder.append("Item [method=");
            builder.append(method);
            builder.append(", path=");
            builder.append(path);
            builder.append(", time=");
            builder.append(time);
            builder.append("]");
            return builder.toString();
        }
    }
    
    private static final Logger logger = LoggerFactory.getLogger(AlarmConfigYaml.class);
    private static AlarmConfigYaml instance;
    
    private List<Item> slowApiConfigItems;
    private boolean enabled = false;
    private boolean listener = true;
    private boolean alarm4xxError = true;
    private boolean alarm5xxError = true;
    private boolean alarmSlowApi = true;
    private List<String> mailTo;
    private List<String> mailCc;
    private String mailFrom;
    private String mailSubject;
    /**
     * 发送周期（秒）
     */
    private long alarmInterval = 30;
    
    public AlarmConfigYaml() {
    }
    
    public synchronized static AlarmConfigYaml load() throws Exception {
        if (instance == null) {
            instance = YamlFileLoader.loadAs("alarm-config.yml", AlarmConfigYaml.class);
            if (instance.isEnabled() && CollectionUtils.isEmpty(instance.getMailTo())) {
                throw new IllegalArgumentException("mailTo missiong");
            }
            if (CollectionUtils.isEmpty(instance.getMailCc())) {
                instance.setMailCc(Collections.emptyList());
            }
        }
        return instance;
    }
    
    public boolean matchRule(String path, String method, long time) {
        if (this.slowApiConfigItems == null || this.slowApiConfigItems.isEmpty()) {
            return false;
        }
        for (Item item : this.slowApiConfigItems) {
            if (item.getMatcher().match(path, method)) {
                logger.trace("matchRule> matched: {}, params:{}, item: {}", time > item.time, time, item);
                return time > item.time;
            }
        }
        
        return false;
    }

    public List<Item> getSlowApiConfigItems() {
        return slowApiConfigItems;
    }

    public void setSlowApiConfigItems(List<Item> slowApiConfigItems) {
        this.slowApiConfigItems = slowApiConfigItems;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isListener() {
        return listener;
    }

    public void setListener(boolean listener) {
        this.listener = listener;
    }

    public boolean isAlarm4xxError() {
        return alarm4xxError;
    }

    public void setAlarm4xxError(boolean alarm4xxError) {
        this.alarm4xxError = alarm4xxError;
    }

    public boolean isAlarm5xxError() {
        return alarm5xxError;
    }

    public void setAlarm5xxError(boolean alarm5xxError) {
        this.alarm5xxError = alarm5xxError;
    }

    public boolean isAlarmSlowApi() {
        return alarmSlowApi;
    }

    public void setAlarmSlowApi(boolean alarmSlowApi) {
        this.alarmSlowApi = alarmSlowApi;
    }

    public List<String> getMailTo() {
        return mailTo;
    }

    public void setMailTo(List<String> mailTo) {
        this.mailTo = mailTo;
    }

    public List<String> getMailCc() {
        return mailCc;
    }

    public void setMailCc(List<String> mailCc) {
        this.mailCc = mailCc;
    }

    public String getMailSubject() {
        return mailSubject;
    }

    public void setMailSubject(String mailSubject) {
        this.mailSubject = mailSubject;
    }

    public String getMailFrom() {
        return mailFrom;
    }

    public void setMailFrom(String mailFrom) {
        this.mailFrom = mailFrom;
    }

    public long getAlarmInterval() {
        return alarmInterval;
    }

    public void setAlarmInterval(long interval) {
        if (interval <= 0) {
            throw new IllegalArgumentException("alarmInterval MUST >= 0");
        }
        this.alarmInterval = interval;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("AlarmConfigYaml [slowApiConfigItems=");
        builder.append(slowApiConfigItems);
        builder.append(", enabled=");
        builder.append(enabled);
        builder.append(", listener=");
        builder.append(listener);
        builder.append(", alarm4xxError=");
        builder.append(alarm4xxError);
        builder.append(", alarm5xxError=");
        builder.append(alarm5xxError);
        builder.append(", alarmSlowApi=");
        builder.append(alarmSlowApi);
        builder.append(", mailTo=");
        builder.append(mailTo);
        builder.append(", mailCc=");
        builder.append(mailCc);
        builder.append(", mailFrom=");
        builder.append(mailFrom);
        builder.append(", mailSubject=");
        builder.append(mailSubject);
        builder.append(", alarmInterval=");
        builder.append(alarmInterval);
        builder.append("]");
        return builder.toString();
    }
}
