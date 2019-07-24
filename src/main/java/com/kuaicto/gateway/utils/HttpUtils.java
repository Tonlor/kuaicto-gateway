package com.kuaicto.gateway.utils;

import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpHeaders;

public abstract class HttpUtils {
//    private static final Logger logger = LoggerFactory.getLogger(HttpUtils.class);
    
    public static String getFirstHeader(HttpHeaders httpHeaders, String headerName) {
        if (httpHeaders == null) {
            return null;
        }
        
        for (Entry<String, List<String>> entry : httpHeaders.entrySet()) {
            if (StringUtils.equalsIgnoreCase(entry.getKey(), headerName)) {
                if (CollectionUtils.isNotEmpty(entry.getValue())) {
                    return entry.getValue().get(0);
                }
                break;
            }
        }
        
        return null;
    }
}
