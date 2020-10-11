# dhp-rpc
 Dynamic header rpc framework
 
 Support multiple network framework switching
 
 ## dhp-rpc-core
 核心框架
 
 ## 客户端使用
 
 ```java
@DhpRpcClientScanner(basePackages="org.dhp.examples.rpcdemo")
```

## 服务端使用
```java
@DhpRpcClientScanner(basePackages="org.dhp.examples.rpcdemo")

```


```

### application.yml 的配置

```yaml
dhp:
  type: Netty # Grizzly(default) Netty 
  port: 6001 # Server port 有该配置就会对外开放dhp服务
  nodes: # downStream nodes
  - name: demo2  # DService(node='demo2')
    host: 127.0.0.1
    port: 6002
```

                               

 
