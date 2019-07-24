package com.kuaicto.gateway.filter;

import java.io.ByteArrayOutputStream;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import com.kuaicto.gateway.Constants;
import com.kuaicto.gateway.alarm.AlarmConfigYaml;
import com.kuaicto.gateway.alarm.ErrorAlarmService;
import com.kuaicto.gateway.alarm.ErrorMsgData;
import com.kuaicto.gateway.utils.NetworkUtils;

/**
 * 错误报警filter
 */
public class GwErrorAlarmGlobalFilter implements GlobalFilter, Ordered {
    protected static final Logger logger = LoggerFactory.getLogger(GwErrorAlarmGlobalFilter.class);

    @Autowired
    private ErrorAlarmService errorAlarm;
    
    private final AlarmConfigYaml alarmConfig;
    public GwErrorAlarmGlobalFilter() throws Exception {
        alarmConfig = AlarmConfigYaml.load();
    }
    
    @Override
    public int getOrder() {
        return -100;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        final long start = System.currentTimeMillis();
        
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        DataBufferFactory dataBufferFactory = response.bufferFactory();

        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(response) {
        @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                // 错误的告警处理
                if (alarmConfig.isAlarm4xxError() && response.getStatusCode().is4xxClientError()) {
                    // 4xx error alarm
                } else if (alarmConfig.isAlarm5xxError() && response.getStatusCode().is5xxServerError()) {
                    // 5xx error alarm
                } else {
                    // no error alarm
                    return super.writeWith(body);
                }
            
                if (body instanceof Flux) {
                    Flux<? extends DataBuffer> flux = (Flux<? extends DataBuffer>) body;

                    return super
                            .writeWith(flux
                                    .buffer()
                                    .map(dataBuffers -> {
                                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                                        dataBuffers.forEach(i -> {
                                            byte[] array = new byte[i.readableByteCount()];
                                            i.read(array);
                                            try {
                                                outputStream.write(array);
                                            } catch (Exception e) {
                                                logger.error("cannot collect error msg", e);;
                                            }
                                        });
                                        
                                        final String rid = response.getHeaders().getFirst(Constants.X_SGW_REQUEST_ID);
                                        
                                        logger.debug("[rid={}] sending alarm: API ERROR @writeWith", rid);

                                        StringBuilder msgBody = new StringBuilder()
                                            .append("<gateway>").append(NetworkUtils.getLocalHostName())
                                            .append("\n<request> ").append(request.getMethodValue()).append(" ").append(request.getURI())
                                            .append("\n<response> ").append(new String(outputStream.toByteArray()));

                                        ErrorMsgData msgData = new ErrorMsgData();
                                        msgData.setRid(rid);
                                        msgData.setSubject("API ERROR");
                                        msgData.setBody(msgBody.toString());
                                        errorAlarm.onError(msgData);
                                        
                                        return dataBufferFactory.wrap(outputStream.toByteArray());
                                    }));
                }
                return super.writeWith(body);
            }
        };

        return chain.filter(exchange.mutate().response(decoratedResponse).build()).doOnError(error -> {
            if (error instanceof java.nio.channels.ClosedChannelException) {
                return;
            }
            if (error instanceof reactor.ipc.netty.channel.AbortedException
                    &&  !(error instanceof reactor.ipc.netty.http.client.HttpClientException)) {
                return;
            }
            
            logger.error("doOnError", error.getMessage(), error);
            final String rid = response.getHeaders().getFirst(Constants.X_SGW_REQUEST_ID);
            logger.debug("[rid={}] sending alarm: API ERROR @doOnError", rid);
            
            String errmsg = StringUtils.isNotBlank(error.getMessage()) ? error.getMessage() : null;
            if (StringUtils.isBlank(errmsg) && error.getCause() != null && StringUtils.isNotBlank(error.getCause().getMessage()) ) {
                errmsg = error.getCause().getMessage();
            }
            errmsg += "\n<stacktrace> " + ExceptionUtils.getFullStackTrace(error);
            
            StringBuilder msgBody = new StringBuilder()
                .append("<gateway>").append(NetworkUtils.getLocalHostName())
                .append("\n<request> ").append(request.getMethodValue()).append(" ").append(request.getURI())
                .append("\n<errmsg> ").append(errmsg);
            
            ErrorMsgData msgData = new ErrorMsgData();
            msgData.setRid(rid);
            msgData.setSubject("API ERROR");
            msgData.setBody(msgBody.toString());
            errorAlarm.onError(msgData);
        }).doOnTerminate(new Runnable() {
            @Override
            public void run() {
                long timeTaken = System.currentTimeMillis() - start;
                String rid = response.getHeaders().getFirst(Constants.X_SGW_REQUEST_ID);
                
                logger.debug("[rid={}] <<Time-Taken>> : {}", rid, timeTaken);
                
                if (alarmConfig.isAlarmSlowApi() && alarmConfig.matchRule(request.getPath().value(), request.getMethodValue(), timeTaken)) {
                    logger.debug("[rid={}] sending alarm: TOO SLOW", rid);
                    
                    StringBuilder msgBody = new StringBuilder()
                        .append("<gateway>").append(NetworkUtils.getLocalHostName())
                        .append("\n<request> ").append(request.getMethodValue()).append(" ").append(request.getURI())
                        .append("\n<Time-Taken> " ).append(timeTaken)
                        .append("\n<reason> TOO SLOW");
                    
                    ErrorMsgData msgData = new ErrorMsgData();
                    msgData.setRid(rid);
                    msgData.setSubject("TOO SLOW");
                    msgData.setBody(msgBody.toString());
                    errorAlarm.onError(msgData);
                }
            }
        });
    }
}
