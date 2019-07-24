package com.kuaicto.gateway.loadbalancer;

import java.io.IOException;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.loadbalancer.Server;

public class PingUrl extends com.netflix.loadbalancer.PingUrl {
    private static final String HEALTH = "/health";
    private static final Logger logger = LoggerFactory.getLogger(PingUrl.class);

    @Override
    public boolean isAlive(Server server) {
        logger.trace("PING: {}", server.getId());
        boolean alive = checkIsAlive(server);

        logger.debug("PING: {}, alive: {}", server.getId(), alive);

        return alive;
    }

    @Override
    public String getPingAppendString() {
        return HEALTH;
    }

    private boolean checkIsAlive(Server server) {
        StringBuilder urlBuilder = new StringBuilder("");
        if (this.isSecure()){
            urlBuilder.append("https://");
        }else{
            urlBuilder.append("http://");
        }
        urlBuilder.append(server.getId());
        urlBuilder.append(getPingAppendString());


        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(urlBuilder.toString());
        httpGet.setConfig(RequestConfig.custom().setSocketTimeout(300).build());
        
        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();
//            boolean isAlive = (statusCode == 200); 
            boolean isAlive = (statusCode < 500); // 用于检查服务是否可用，路径未必真的存在
            if (getExpectedContent()!=null){
                String content = EntityUtils.toString(response.getEntity());
                logger.debug("content:" + content);
                if (content == null){
                    isAlive = false;
                }else{
                    isAlive = content.equals(getExpectedContent());
                }
            }
            return isAlive;
        } catch (Exception e) {
            logger.error(e.getMessage());
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
//                    logger.error(e.getMessage(), e);
                }
            }
            try {
                httpClient.close();
            } catch (IOException e) {
//                logger.error(e.getMessage(), e);
            }
        }

        return false;
    }
}
