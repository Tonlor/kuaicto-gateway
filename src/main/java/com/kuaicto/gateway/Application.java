package com.kuaicto.gateway;

import java.io.IOException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import com.kuaicto.gateway.alarm.ErrorAlarmService;
import com.kuaicto.gateway.config.GatewayConfig;
import com.kuaicto.gateway.filter.GwAuthGatewayFilterFactory;
import com.kuaicto.gateway.filter.GwDefaultGatewayFilterFactory;
import com.kuaicto.gateway.filter.GwDefaultGlobalFilter;
import com.kuaicto.gateway.filter.GwErrorAlarmGlobalFilter;
import com.kuaicto.gateway.filter.GwProfileGatewayFilterFactory;
import com.kuaicto.gateway.filter.GwRefuseAnonymousGatewayFilterFactory;
import com.kuaicto.gateway.filter.GwRequestRateLimiterGlobalFilter;
import com.kuaicto.gateway.filter.GwRewritePathGatewayFilterFactory;
import com.kuaicto.gateway.filter.GwRoleGatewayFilterFactory;
import com.kuaicto.gateway.filter.GwSetPathGatewayFilterFactory;
import com.kuaicto.gateway.limiter.RequestLimiterCenter;
import com.kuaicto.gateway.utils.RestClient;

@SpringBootApplication
@EnableAutoConfiguration
@EnableDiscoveryClient
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
	
	@Bean
	public RestClient restClient() {
		return new RestClient();
	}
	
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory)
    {
//        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<Object>(Object.class);
//        ObjectMapper om = new ObjectMapper();
//        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
//        om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
//        jackson2JsonRedisSerializer.setObjectMapper(om);
        RedisTemplate<String, Object> template = new RedisTemplate<String, Object>();
        template.setConnectionFactory(redisConnectionFactory);
//        template.setKeySerializer(jackson2JsonRedisSerializer);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new Jackson2JsonRedisSerializer<Object>(Object.class));
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new Jackson2JsonRedisSerializer<Object>(Object.class));
        template.afterPropertiesSet();
        return template;
    }

	@Bean
	public GwDefaultGatewayFilterFactory gwDefaultGatewayFilterFactory() {
		return new GwDefaultGatewayFilterFactory();
	}

	// RateLimiter: START
    @Bean
    public RequestLimiterCenter requestLimiterCenter(StringRedisTemplate stringRedisTemplate, ErrorAlarmService alarmService) throws Exception {
        return new RequestLimiterCenter(stringRedisTemplate, alarmService);
    }
    
    @Bean
    public GwRequestRateLimiterGlobalFilter gwRequestRateLimiterGlobalFilter() {
        return new GwRequestRateLimiterGlobalFilter();
    }
    // RateLimiter: END

	
	@Bean
	public GwRefuseAnonymousGatewayFilterFactory gwRefuseAnonymousGatewayFilterFactory() {
		return new GwRefuseAnonymousGatewayFilterFactory();
	}

	@Bean
	public GwRoleGatewayFilterFactory gwRoleGatewayFilterFactory() {
		return new GwRoleGatewayFilterFactory();
	}
	@Bean
	public GwProfileGatewayFilterFactory gwProfileGatewayFilterFactory(StringRedisTemplate redisTemplate) {
		return new GwProfileGatewayFilterFactory(redisTemplate);
	}
	@Bean
	public GwAuthGatewayFilterFactory gwAuthGatewayFilterFactory() {
		return new GwAuthGatewayFilterFactory();
	}
	@Bean
	public GwAuthGatewayFilterFactory gwPortalAuthGatewayFilterFactory() {
		return new GwAuthGatewayFilterFactory();
	}
	@Bean
	public GwSetPathGatewayFilterFactory gwSetPathGatewayFilterFactory() {
		return new GwSetPathGatewayFilterFactory();
	}
	@Bean
	public GwRewritePathGatewayFilterFactory gwRewritePathGatewayFilterFactory() {
		return new GwRewritePathGatewayFilterFactory();
	}
    
//    @Bean
//    public GwProxyCacheGlobalFilter gwProxyCacheGlobalFilter(StringRedisTemplate template) throws Exception {
//        return new GwProxyCacheGlobalFilter(template);
//    }
    
    @Bean
    public GwDefaultGlobalFilter gwDefaultGlobalFilter(GatewayConfig gatewayConfig, ErrorAlarmService errorAlarm) {
        try {
            return new GwDefaultGlobalFilter(gatewayConfig, errorAlarm);
        } catch (Exception e) {
            System.err.println("System Expired.");
            System.exit(1);
            return null;
        }
    }
    
    
    @Bean
    public ErrorAlarmService errorAlarmService(StringRedisTemplate redisTemplate, JavaMailSender mailSender, GatewayConfig gatewayConfig) throws Exception {
        return new ErrorAlarmService(redisTemplate, mailSender, gatewayConfig);
    }
    
    @Bean
    public GwErrorAlarmGlobalFilter gwErrorAlarmGlobalFilter() throws Exception {
        return new GwErrorAlarmGlobalFilter();
    }
	
	@Bean
	@LoadBalanced
	public RestTemplate newRestTemplate() {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.setErrorHandler(new ResponseErrorHandler(){
			@Override
			public boolean hasError(ClientHttpResponse response)
					throws IOException {
				return false;
			}
			@Override
			public void handleError(ClientHttpResponse response)
					throws IOException {
			}
		});
		return restTemplate;
	}
}
