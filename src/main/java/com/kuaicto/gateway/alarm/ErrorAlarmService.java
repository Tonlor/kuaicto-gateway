package com.kuaicto.gateway.alarm;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PreDestroy;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands.SetOption;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import com.alibaba.fastjson.JSON;
import com.kuaicto.gateway.config.GatewayConfig;

/**
 * 基于IP的限流
 */
public class ErrorAlarmService implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ErrorAlarmService.class);
    private static final ExecutorService mainExecutor = Executors.newSingleThreadExecutor();
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    
    private static final int TIMEOUT_SECONDS = 30;

    public static final String MSG_DATA_QUQUE = "gw.error::ErrorMsgData";
    private static final byte[] MSG_DATA_QUQUE_AS_BYTES = MSG_DATA_QUQUE.getBytes();

    private final JavaMailSender mailSender;
    
    private final StringRedisTemplate redisTemplate;
    private final AtomicBoolean isShutdown = new AtomicBoolean(false);
    private final AlarmConfigYaml alarmConfig;
    
    public ErrorAlarmService(StringRedisTemplate redisTemplate, JavaMailSender mailSender, GatewayConfig gatewayConfig) throws Exception {
        this.redisTemplate = redisTemplate;
        this.mailSender = mailSender;
        
        alarmConfig = AlarmConfigYaml.load();
        
        if (alarmConfig.isListener()) {
            mainExecutor.execute(this);
        }
    }

    @Override
    public void run() {
        RedisConnection connection = redisTemplate.getConnectionFactory().getConnection();
        
        while (!isShutdown.get()) {
            try {
                if (connection.isClosed()) {
                    logger.info("redis connection closed. will get a new connection");
                    connection = redisTemplate.getConnectionFactory().getConnection();
                }
                
                // 使用redis控制频率，多客户端场景 
                while (!isShutdown.get() && !connection.set("gw.error.lock".getBytes(), "Y".getBytes(), Expiration.seconds(alarmConfig.getAlarmInterval()), SetOption.SET_IF_ABSENT)) {
                    Thread.sleep(alarmConfig.getAlarmInterval() * 1000); // sleep
                }
                
                List<byte[]> list = connection.bLPop(TIMEOUT_SECONDS, MSG_DATA_QUQUE_AS_BYTES);
                if (CollectionUtils.isEmpty(list)) {
                    continue;
                }
                
                String text = new String(list.get(1)); // 0->key, 1->value
                List<ErrorMsgData> errors = new ArrayList<>();
                errors.add(JSON.parseObject(text, ErrorMsgData.class));
                
                for (int i = 0 ; !isShutdown.get() && i < 300; i++) {
                    byte[] bytes = connection.lPop(MSG_DATA_QUQUE_AS_BYTES);
                    if (bytes == null) {
                        break;
                    }
                    errors.add(JSON.parseObject(new String(bytes), ErrorMsgData.class));
                }
                
                onReceived(errors);
            } catch (Exception e) {
                if (e.getCause() instanceof CancellationException) {
                    // ignore
                } else {
                    logger.error(e.getMessage(), e);
                }
            }
        }
        
        connection.close();
    }

    @PreDestroy
    public void shutdown() {
        isShutdown.set(true);
        
        mainExecutor.shutdown();
        executor.shutdown();
    }
    
    /**
     * 处理检查
     * @param msgData
     */
    public boolean onError(ErrorMsgData msgData) {
        if (!alarmConfig.isEnabled()) {
            return false;
        }
        
        if (alarmConfig.isAlarmSlowApi() 
                || this.alarmConfig.isAlarm4xxError() 
                || this.alarmConfig.isAlarm5xxError()) {
            executor.execute(new Runnable(){
                @Override
                public void run() {
                    String jsonString = JSON.toJSONString(msgData);
                    redisTemplate.boundListOps(ErrorAlarmService.MSG_DATA_QUQUE).rightPush(jsonString);
                }
            });
            
            return true;
        }
        
        return false;
    }
     
    /**
     * 接受请求并处理
     * @param errors
     * @return
     */
    private boolean onReceived(List<ErrorMsgData> errors) {
        logger.info("onReceived: {}", errors);
        
        if (errors == null || errors.isEmpty()) {
            return false;
        }
        
        Set<String> subjects = new LinkedHashSet<>();
        subjects.add(alarmConfig.getMailSubject() + " (" + errors.size() + ")");

        StringBuilder sb = new StringBuilder();
        int seq = 0;
        for (ErrorMsgData error : errors) {
            sb.append("\n").append(++seq).append(") ##rid: ").append(error.getRid()).append("##");
            sb.append("\n").append(error.getBody());
            sb.append("\n\n");
         
            if (StringUtils.isNotBlank(error.getSubject())) {
                subjects.add(error.getSubject());
            }
        }
        
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(alarmConfig.getMailFrom());
        msg.setTo(alarmConfig.getMailTo().toArray(new String[0]));
        msg.setCc(alarmConfig.getMailCc().toArray(new String[0]));
        msg.setSubject(StringUtils.join(subjects, ", ") +  " - " + new Date());
        msg.setText(sb.toString());
        
        this.mailSender.send(msg);
        
        return true;
    }

}
