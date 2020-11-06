package org.dhp.core.rpc;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.dhp.core.spring.DhpProperties;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;

import javax.annotation.Resource;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 关于IO通讯的连接池，一般来说设定好固定连接池就可以，不是越多越好
 * 所以每个下游节点维护好固定数量的Channel就行
 *
 * @author zhangcb
 */
@Slf4j
public class RpcChannelPool implements InitializingBean, BeanFactoryAware {

    protected Set<RpcChannel> readyToCloseChannels = ConcurrentHashMap.newKeySet();

    @Resource
    DhpProperties properties;

    protected Map<String, RpcChannel[]> allChannels = new ConcurrentHashMap<>();

    public Map<String, RpcChannel[]> getAllChannels() {
        return allChannels;
    }

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
        RpcChannel[] channels;
        if (!allChannels.containsKey(node.getName())) {
            channels = new RpcChannel[node.getChannelSize()];
            RpcChannel[] old = allChannels.putIfAbsent(node.getName(), channels);
            if (old != null) {
                channels = old;
            }
        } else {
            channels = allChannels.get(node.getName());
        }
        int len = channels.length;
        int randomIndex = RandomUtils.nextInt(0, len);
        for (int index = randomIndex; index < len + randomIndex; index++) {
            RpcChannel channel = channels[index % len];
            if (channel == null) {
                synchronized (node.getName().intern()) {
                    channel = new RpcChannelBuilder()
                            .setHost(node.getHost())
                            .setPort(node.getPort())
                            .setName(node.getName())
                            .setTimeout(node.getTimeout())
                            .setType(properties.getType())
                            .build();
                    channel.start();
                    channels[index % len] = channel;
                    return channel;
                }
            }
            //在有连接可用的前提下，直接返回可用的channel
            if (channel.isActive()) {
                return channel;
            } else {
                channels[index % len] = null;
                //当前channel丢入准备关闭的channels列表里面
                readyToCloseChannels.add(channel);
            }
        }
        throw new RpcException(RpcErrorCode.UNREACHABLE_NODE);
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
                    allChannels.values().stream().forEach(rpcChannels -> {
                        for (RpcChannel channel : rpcChannels) {
                            if (channel != null) {
                                channel.ping();
                            }
                        }
                    });
                    //检查所有待关闭的channel，如果已经关闭的就移除，如果未活跃的连接，1分钟后关闭并移除
                    readyToCloseChannels.removeIf(rpcChannel -> {
                        if (rpcChannel.isClose()) {
                            return true;
                        }
                        if (rpcChannel.activeTime < System.currentTimeMillis() - 600000) {
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
