package com.kuaicto.gateway.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;

public abstract class NetworkUtils {
    
    private static final String hostName;
    static {
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static String getLocalHostName() {
        return hostName;
    }
}
