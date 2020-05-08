package org.dhp.core.rpc;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RpcChannelPool {

    protected Map<String, RpcChannel> allChannels = new ConcurrentHashMap<>();

    public RpcChannel getChannel(Node node) {
        //if existed
        if(allChannels.containsKey(node.getName())){
            return allChannels.get(node.getName());
        }
        RpcChannel channel = new RpcChannelBuilder().setHost(node.getHost()).build();
        RpcChannel old = allChannels.putIfAbsent(node.getName(), channel);
        if(old != null){
            channel = old;
        }
        //加入到BeanFactory
        return channel;
    }

}
