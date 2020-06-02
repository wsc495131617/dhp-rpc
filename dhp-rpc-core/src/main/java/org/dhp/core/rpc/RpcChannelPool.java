package org.dhp.core.rpc;

import lombok.extern.slf4j.Slf4j;
import org.dhp.core.spring.DhpProperties;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zhangcb
 */
@Slf4j
public class RpcChannelPool implements InitializingBean, BeanFactoryAware {
    
    protected Map<String, RpcChannel> allChannels = new ConcurrentHashMap<>();
    
    protected Node getNode(Command command) {
        if (command.getNodeName() != null && DhpProperties.getInstance().getNodes() != null) {
            for (Node node : DhpProperties.getInstance().getNodes()) {
                //如果
                if (node.getName().equals(command.getNodeName())) {
                    return node;
                }
            }
        }
        return null;
    }
    
    public RpcChannel getChannel(Command command) {
        Node node = getNode(command);
        if (node == null) {
            throw new RpcException(RpcErrorCode.NODE_NOT_FOUND);
        }
        //if existed
        if (allChannels.containsKey(node.getName())) {
            return allChannels.get(node.getName());
        }
        RpcChannel channel = new RpcChannelBuilder()
                .setHost(node.getHost())
                .setPort(node.getPort())
                .setName(node.getName())
                .setTimeout(node.getTimeout())
                .setType(DhpProperties.getInstance().getType())
                .build();
        RpcChannel old = allChannels.putIfAbsent(node.getName(), channel);
        if (old != null) {
            channel = old;
        }
        //加入到BeanFactory
        return channel;
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
