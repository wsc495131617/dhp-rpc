# dhp-rpc
 Dynamic header rpc framework
 
 Support multiple network framework switching
 
 ## dhp-rpc-core
 核心框架
 
 ### 客户端使用
 
 ```java
@DhpRpcClientScanner(basePackages="org.dhp.examples.rpcdemo")
```

### 服务端使用
```java
@DhpRpcClientScanner(basePackages="org.dhp.examples.rpcdemo")

```

### 底层网络通信模式
1. Grizzly
   - 作为微服务内部通讯相对可控的框架，性能一般配置下币Netty要好
2. Netty
   - 需要做优化
3. NIO
   - 简单通过单Selector实现，内网通讯足够用了
### 底层线程模型
1. 网络通信层
   - 负责消息包的接受和发送
   - 负责断开重连，心跳机制等
2. 线程执行调度层
   - 负责调度所有业务功能
   - 弹性智能调度模型，检测耗时较大的功能，单独开辟慢处理线程来处理，不影响其他正常业务
   - 单独慢处理线程一个功能号只有一个，性能问题通过多实例解决


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

                          
                          
# dhp-distribution
## ZookeeperUtils
```yaml
#依赖配置
zookeeper:
  connectUrl: ""
```     
手工加Bean
```java
//...
@Bean
public ZookeeperUtils zookeeperUtils() {
    return new ZookeeperUtils();
}
```

```java
@ZKLocked
//依赖注解，直接可以对Bean的某个方法进行分布式锁

//也可以手工
ZookeeperLock lock = zookeeperUtils.createLock(methodName.replace(".","_"));
try {
    lock.lock();
    //do somethings
} finally {
    lock.unlock();
}

```
## LockUtils
通过Redis实现的锁工具，有两类乐观锁机制
1. trySet
   trySet是不需要释放，用于控制缓存值的修改，比如秒杀，先获取上一次查询的余额，然后减去数量后更新，看是否成功，成功就执行后续操作。
2. tryLock
    普通乐观锁，但是需要控制超时时间，否则乐观锁机制会有问题，控不住
 
