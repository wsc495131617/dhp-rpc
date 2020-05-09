package org.dhp.core.rpc;

import org.dhp.core.spring.DhpProperties;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RpcChannelPool {

    @Resource
    DhpProperties properties;

    protected Map<String, RpcChannel> allChannels = new ConcurrentHashMap<>();

    public RpcChannel getChannel(Node node) {
        //if existed
        if(allChannels.containsKey(node.getName())){
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
        if(old != null){
            channel = old;
        }
        //加入到BeanFactory
        return channel;
    }

}
