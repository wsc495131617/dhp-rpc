package org.dhp.core.rpc;

import lombok.extern.slf4j.Slf4j;
import org.dhp.core.spring.DhpProperties;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zhangcb
 */
@Slf4j
public class RpcChannelPool implements InitializingBean, BeanFactoryAware {

    @Resource
    DhpProperties properties;
    
    protected Map<String, RpcChannel> allChannels = new ConcurrentHashMap<>();
    
    protected Node getNode(Command command) {
        if (command.getNodeName() != null && properties.getNodes() != null) {
            for (Node node : properties.getNodes()) {
                //如果
                if (node.getName().equals(command.getNodeName())) {
                    return node;
                }
            }
        }
        return null;
    }

    /**
     * 需要确保并发有效
     * @param command
     * @return
     */
    public RpcChannel getChannel(Command command) {
        Node node = getNode(command);
        if (node == null) {
            throw new RpcException(RpcErrorCode.NODE_NOT_FOUND);
        }
        synchronized (node.getName().intern()){
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
            channel.start();
            allChannels.put(node.getName(), channel);
            return channel;
        }
    }
    
    BeanFactory beanFactory;
    
    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }
    
    @Override
    public void afterPropertiesSet() throws Exception {
        Thread t = new Thread(() -> {
            while (true) {
                try {
                    for (Map.Entry<String, RpcChannel> entry : allChannels.entrySet()) {
                        try {
                            entry.getValue().ping();
                        } catch (Throwable e) {
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
