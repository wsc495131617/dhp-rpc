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
```properties
dhp.port = 9001 # port > 0

```

### 底层网络通信模式
1. Grizzly
   - 作为微服务内部通讯相对可控的框架，性能比Netty要好一些，主要原因是内存和线程模式的优化
2. Netty
   - 需要做优化，比如调整IOSelector的线程池数量
3. NIO
   - 服务端自建了Boss线程用于Accept Socket，可以配置NIOSelector线程用于接受请求和处理
4. ZMQ
   todo
5. AIO
   todo
### 底层线程模型
1. 网络通信层(Grizzly, Netty, NIO)
   - 负责消息包的接受和发送
   - 负责断开重连，心跳机制等
2. 线程执行调度层（Workers)
   - 负责调度所有业务功能
   - 弹性智能调度模型，检测耗时较大的功能，单独开辟慢处理线程来处理，不影响其他正常业务
   - 单独慢处理线程一个功能号只有一个，性能问题通过多实例解决

### 主从模式
1. 配置
~~~yaml
dhp:
  lb:
    ha: cluster | ms （集群还是主从）
~~~
2. 配合HaSupport方法进行切换控制,以下方法写在程序关闭前的钩子内，或者手工关闭主的方法体内
~~~java
//关闭节点，避免新的请求进入
nodeCenter.close();
//放弃主的身份
if (haSupport.giveUpMaster()) {
    //... 编写同步数据等处理
    // 同步方法完成后关闭主
    haSupport.close();
}
~~~


### application.yml 的配置

```yaml
dhp:
  type: Netty # Grizzly(default) Netty NIO
  port: 6001 # Server port 有该配置就会对外开放dhp服务
  nodes: # downStream nodes
  - name: demo2  # DService(node='demo2')
    host: 127.0.0.1
    port: 6002
  lb:
    enable: true
    zk_url: localhost:2181 # 通过zk作为注册中心
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
 
