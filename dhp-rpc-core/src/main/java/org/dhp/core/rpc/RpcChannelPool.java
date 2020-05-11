package org.dhp.core.rpc;

import lombok.extern.slf4j.Slf4j;
import org.dhp.core.spring.DhpProperties;
import org.springframework.beans.factory.InitializingBean;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class RpcChannelPool implements InitializingBean {

    @Resource
    DhpProperties properties;

    protected Map<String, RpcChannel> allChannels = new ConcurrentHashMap<>();

    public RpcChannel getChannel(Node node) {
        //if existed
        if (allChannels.containsKey(node.getName())) {
            return allChannels.get(node.getName());
        }
        RpcChannel channel = new RpcChannelBuilder()
                .setHost(node.getHost())
                .setPort(node.getPort())
                .setName(node.getName())
                .setTimeout(node.getTimeout())
                .setType(properties.getType())
                .build();
        RpcChannel old = allChannels.putIfAbsent(node.getName(), channel);
        if (old != null) {
            channel = old;
        }
        //加入到BeanFactory
        return channel;
    }

    public void afterPropertiesSet() throws Exception {
        Thread t = new Thread(()->{
            while(true){
                try{
                    for(Map.Entry<String, RpcChannel> entry : allChannels.entrySet()){
                        try {
                            entry.getValue().ping();
                        } catch (Throwable e){
                            log.error(e.getMessage(), e);
                        }
                    }
                    //15秒发起心跳
                    Thread.sleep(15000);
                } catch (Throwable e) {
                    log.error(e.getMessage(), e);
                }
            }
        });
        t.setName("RpcChannelPool");
        t.setDaemon(true);
        t.start();
    }
}
