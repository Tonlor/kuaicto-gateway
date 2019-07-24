## 打包
> mvn clean package 

## 发布
* 目录：target
* target/*.jar
* target/lib/*.jar



## 配置项
### 内置Filter
[参考 spring-cloud-gateway.adoc](https://github.com/spring-cloud/spring-cloud-gateway/blob/master/docs/src/main/asciidoc/spring-cloud-gateway.adoc)

### 扩展Filter
```
  * 默认设置，包含请求ID：X-SGW-REQUEST-ID
  - GwDefault

  * 获取用户信息，并设置Header：X-SESSION-USER, X-SESSION-USER-ENCODED
  - GwProfile=lb://user-service/auth/profile
  
  * 根据权限菜单拦截API访问的配置
  - GwAuth=lb://permission-service/permission/check, /api/:!/api/login:!/api/register
  
  * 获取角色，设置Header: X-SGW-SESSION-ROLES
  - GwRole=lb://role-service/session/role

  * 拒绝匿名访问的配置
  - GwRefuseAnonymous=/api/:!/api/login:!/api/register
  
  * 重写路径，内置Filter RewritePath无法支持中文路径
  - GwRewritePath=/api/(?<segment>.*), /$\{segment}
  - GwRewritePath=/api/([^/]+)/(?<segment>.*), /$\{segment}

  * 设置路径，内置Filter SetPath无法支持中文路径  
  - SetPath=/{segment}

```

## 示例
***Spring application.yml示例***

<pre>

server:
  port: 8888

spring:
  application:
    name: kuaicto-gateway
  cloud:
    gateway:
      default-filters:
      - AddResponseHeader=X-SGW-Version, 1.0
      routes:
      - id: fashion_api_route
        uri: http://web-admin.fashion.dev.sudaotech.com
        order: 0
        predicates:
        #- Path=/api/fashion/{segment}
        - Path=/api/fashion/**
        filters:
          - Auth=lb://portal-manager-api/authApi
          #- GwSetPath=/{segment}
          - GwRewritePath=/api/fashion/(?<segment>.*), /$\{segment}
      # =====================================
      - id: portal_api_route
        uri: lb://portal-manager-api
        order: 0
        predicates:
#       - Path=/api/portal/{segment}
        - Path=/api/portal/**
        filters:
#         - GwSetPath=/{segment}
          - GwRewritePath=/api/portal/(?<segment>.*), /$\{segment}
      # =====================================          

eureka:
  client:
    serviceUrl:
      defaultZone: http://172.19.71.10:8761/eureka/
  instance:
    preferIpAddress: true

logging:
  level:
    org.springframework.cloud.gateway: TRACE
    org.springframework.http.server.reactive: DEBUG
    org.springframework.web.reactive: DEBUG
    reactor.ipc.netty: DEBUG
    com.kuaicto.gateway: DEBUG

</pre>

***关键字段说明：***

* routes.uri 业务服务API
* filters.Auth 权限校验API



## 运行
***基于target目录的示例***
<pre>
for jar in lib/*.jar; do
  GW_CLASSPATH=$GW_CLASSPATH:$jar
done

GW_CLASSPATH=$GW_CLASSPATH:kuaicto-gateway-1.0.jar

java -cp $GW_CLASSPATH com.kuaicto.gateway.Application --spring.config.location=application-custom.yml

</pre>


## FAQ
***@Martin Support***
