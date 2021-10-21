package org.dhp.core.rpc;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.dhp.core.spring.DhpProperties;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
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

    protected Map<Node, RpcChannel[]> allChannels = new ConcurrentHashMap<>();

    public Map<Node, RpcChannel[]> getAllChannels() {
        return allChannels;
    }

    public List<Node> getNodes(String nodeName) {
        List<Node> nodes = Lists.newLinkedList();
        if (nodeName != null && properties.getNodes() != null) {
            for (Node node : properties.getNodes()) {
                //如果
                if (node.getName().equals(nodeName)) {
                    nodes.add(node);
                }
            }
        }
        return nodes;
    }

    protected Node getMasterNode(String nodeName) {
        TreeSet<Node> nodes = new TreeSet<>();
        if (nodeName != null && properties.getNodes() != null) {
            for (Node node : properties.getNodes()) {
                //如果
                if (node.isEnable() && node.getName().equals(nodeName)) {
                    nodes.add(node);
                }
            }
        }
        return nodes.pollFirst();
    }

    /**
     * 根据nodeName获取
     * @param nodeName
     * @return
     */
    public RpcChannel getChannel(String nodeName) {
        Node node = getMasterNode(nodeName);
        if (node == null) {
            throw new RpcException(RpcErrorCode.NODE_NOT_FOUND);
        }
        return getChannel(node);
    }

    /**
     * 需要确保并发有效
     *
     * @param node
     * @return
     */
    public RpcChannel getChannel(Node node) {
        //if existed
        RpcChannel[] channels;
        if (!allChannels.containsKey(node)) {
            channels = new RpcChannel[node.getChannelSize()];
            RpcChannel[] old = allChannels.putIfAbsent(node, channels);
            if (old != null) {
                channels = old;
            }
        } else {
            channels = allChannels.get(node);
        }
        int len = channels.length;
        int randomIndex = (int) (Thread.currentThread().getId() % len);
        for (int index = randomIndex; index < len + randomIndex; index++) {
            RpcChannel channel = channels[index % len];
            if (channel == null) {
                //每个Channel创建都需要指定node进行锁定
                synchronized (node) {
                    if (channels[index % len] == null) {
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
                    } else {
                        return channels[index % len];
                    }
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
        log.warn("unreachable {}", node);
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
                            rpcChannel.close();
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
                    //异常之后3秒重试
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException interruptedException) {
                    }
                }
            }
        });
        t.setName("RpcChannelPool");
        t.setDaemon(true);
        t.start();
    }
}
