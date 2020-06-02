# dhp-rpc
 Dynamic header rpc framework
 
 Support multiple network framework switching
 
 ## dhp-rpc-core
 核心框架
 
 ## 客户端使用
 
 ```java
@EnableConfigurationProperties(DhpProperties.class)
@EnableDhpRpcClient(basePackages="org.dhp.examples.rpcdemo")
```

## 服务端使用
```java
@EnableConfigurationProperties(DhpProperties.class)
@EnableDhpRpcServer
```

## 既开放了服务端，又需要调用别的服务作为客户端
```java
@EnableConfigurationProperties(DhpProperties.class)
@EnableDhpRpcClient
@EnableDhpRpcServer

```

### application.yml 的配置

```yaml
dhp:
  type: Netty # Grizzly(default) Netty 
  port: 6001 # Server port
  nodes: # downStream nodes
  - name: demo2  # DService(node='demo2')
    host: 127.0.0.1
    port: 6002
```

                               

 
