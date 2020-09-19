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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zhangcb
 */
@Slf4j
public class RpcChannelPool implements InitializingBean, BeanFactoryAware {

    protected Set<RpcChannel> readyToCloseChannels = ConcurrentHashMap.newKeySet();

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
     *
     * @param command
     * @return
     */
    public RpcChannel getChannel(Command command) {
        Node node = getNode(command);
        if (node == null) {
            throw new RpcException(RpcErrorCode.NODE_NOT_FOUND);
        }
        //if existed
        if (allChannels.containsKey(node.getName())) {
            RpcChannel channel = allChannels.get(node.getName());
            //在有连接可用的前提下，直接返回可用的channel
            if (channel.isActive()) {
                return channel;
            } else {
                //当前channel丢入准备关闭的channels列表里面
                readyToCloseChannels.add(allChannels.remove(node.getName()));
            }
        }
        synchronized (node.getName().intern()) {
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
                    //ping所有的连接
                    allChannels.values().stream().forEach(RpcChannel::ping);
                    //检查所有待关闭的channel，如果已经关闭的就移除，如果未活跃的连接，1分钟后关闭并移除
                    readyToCloseChannels.removeIf(rpcChannel -> {
                        if (rpcChannel.isClose()) {
                            return true;
                        }
                        if ( rpcChannel.activeTime<System.currentTimeMillis() - 600000) {
                            rpcChannel.close();
                            return true;
                        }
                        return false;
                    });
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
