package org.dhp.lb;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.zookeeper.*;
import org.dhp.common.utils.JacksonUtil;
import org.dhp.common.utils.LocalIPUtils;
import org.dhp.common.utils.SystemInfoUtils;
import org.dhp.core.rpc.Node;
import org.dhp.core.rpc.RpcErrorCode;
import org.dhp.core.rpc.RpcException;
import org.dhp.core.spring.DhpProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;

/**
 * Node注册中心，如果开启注册中心，那么所有dhp的下游节点，都通过注册中心来查找
 */
@Slf4j
@Data
public class NodeCenter implements Watcher {

    /**
     * 集群名称
     */
    @Value("${dhp.lb.name:defaultCluster}")
    String clusterName;

    @Value("${dhp.lb.zk_url}")
    String zkUrl;

    @Resource
    DhpProperties dhpProperties;

    @Resource
    Environment environment;

    String currentPath;

    CountDownLatch connectedSemaphore;

    ZooKeeper zk;

    @PostConstruct
    public void init() throws Exception {
        zk = new ZooKeeper(zkUrl, 5000, this);
        connectedSemaphore = new CountDownLatch(1);
        connectedSemaphore.await();
        if (dhpProperties.getPort() > 0) {
            //创建集群根目录
            createPath("/" + clusterName, new byte[0], CreateMode.PERSISTENT, "create cluster path");
            //创建节点临时
            NodeStatus tmp = new NodeStatus();
            String host = (StringUtils.isEmpty(dhpProperties.getHost()) ? LocalIPUtils.resolveIp() : dhpProperties.getHost());
            currentPath = "/" + clusterName + "/" + dhpProperties.getName() + "_" + host + ":" + dhpProperties.getPort();
            if (dhpProperties.getName() != null) {
                tmp.setName(dhpProperties.getName());
            } else {
                String appName = environment.getProperty("spring.application.name");
                if (appName != null) {
                    tmp.setName(appName);
                } else {
                    throw new RpcException(RpcErrorCode.SYSTEM_ERROR);
                }
            }
            tmp.setPath(currentPath);
            tmp.setHost(host);
            tmp.setPort(dhpProperties.getPort());
            createPath(currentPath, JacksonUtil.bean2JsonBytes(tmp), CreateMode.EPHEMERAL, "create node");
            current = tmp;
        } else {
            try {
                List<String> list = zk.getChildren("/" + clusterName, false);
                for (String path : list) {
                    updateNextNode("/" + clusterName + "/" + path);
                }
            } catch (KeeperException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        zk.addWatch("/" + clusterName, this, AddWatchMode.PERSISTENT_RECURSIVE);
        Thread t = new Thread(() -> {
            while (true) {
                try {
                    updateNode();
                    Thread.sleep(5000);
                } catch (Exception e) {
                    log.error("updateNode error", e);
                }
            }
        });
        t.start();
    }

    NodeStatus current;

    /**
     * 5s更新一次节点信息
     */
    public void updateNode() {
        if (current == null) {
            return;
        }
        current.setCpuLoad(SystemInfoUtils.getProcessCpuLoad() + SystemInfoUtils.getSystemCpuLoad());
        current.setMemLoad((double) SystemInfoUtils.getUsedMemory() / (double) SystemInfoUtils.getTotalMemorySize());
        current.setTotalLoad((current.getCpuLoad() + current.getMemLoad()) / 2);
        try {
            zk.setData(currentPath, JacksonUtil.bean2JsonBytes(current), -1);
        } catch (Throwable e) {
            log.error("update Zk Error");
        }
    }

    public void updateNextNode(String path) {
        try {
            byte[] content = zk.getData(path, false, null);
            NodeStatus nodeStatus = JacksonUtil.bytes2Bean(content, NodeStatus.class);
            Vector<Node> nodes = dhpProperties.getNodes();
            if (nodes == null) {
                nodes = new Vector<>();
                dhpProperties.setNodes(nodes);
            }
            boolean hasNode = false;
            for (Node node : nodes) {
                if (node.getPath().equalsIgnoreCase(nodeStatus.getPath())) {
                    if (nodeStatus.getCpuLoad() != null && nodeStatus.getCpuLoad() > 0) {
                        node.setWeight(1 / nodeStatus.getCpuLoad());
                    } else {
                        node.setWeight(0.001);
                    }
                    hasNode = true;
                }
            }
            if (!hasNode) {
                Node node = new Node();
                node.setName(nodeStatus.getName());
                node.setHost(nodeStatus.getHost());
                node.setPort(nodeStatus.getPort());
                if (nodeStatus.getCpuLoad() != null && nodeStatus.getCpuLoad() > 0) {
                    node.setWeight(1 / nodeStatus.getCpuLoad());
                } else {
                    node.setWeight(0.001);
                }
                node.setPath(path);
                node.setTimeout(5000);
                nodes.add(node);
                log.info("add next node: {}", node);
            }
        } catch (KeeperException | InterruptedException e) {
            log.error("updateNextNode: {} error", path, e);
        }
    }

    protected void createPath(String path, byte[] content, CreateMode mode, String context) {
        zk.create(path, content, ZooDefs.Ids.OPEN_ACL_UNSAFE, mode, new AsyncCallback.StringCallback() {
            @Override
            public void processResult(int i, String s, Object o, String s1) {
                CreateMode createMode = null;
                try {
                    createMode = CreateMode.fromFlag(i);
                } catch (KeeperException e) {
                }
                if (i == 0) {
                    log.info("{} {}, {}", createMode, o, s);
                } else {
                    log.info("{} {},{},{},{}", createMode, i, s, o, s1);
                }
            }
        }, context);
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        if (Event.KeeperState.SyncConnected == watchedEvent.getState()) {
            if (connectedSemaphore.getCount() > 0) {
                connectedSemaphore.countDown();
            }
            //节点创建或者内容更新
            if (Event.EventType.NodeCreated == watchedEvent.getType() || Event.EventType.NodeDataChanged == watchedEvent.getType()) {
                String path = watchedEvent.getPath();
                if (path.equalsIgnoreCase(currentPath)) {
                    return;
                }
                updateNextNode(path);
            } else if (Event.EventType.NodeDeleted == watchedEvent.getType()) {
                List<Node> nodes = dhpProperties.getNodes();
                if (nodes == null || nodes.isEmpty()) {
                    return;
                }
                HashSet<Node> deleteNodes = new HashSet<>();
                for (Node node : nodes) {
                    if (node.getPath().equalsIgnoreCase(watchedEvent.getPath())) {
                        deleteNodes.add(node);
                    }
                }
                deleteNodes.forEach(nodes::remove);
            } else {
                log.info("watched other event: {}", watchedEvent);
            }
        } else if (Event.KeeperState.Expired == watchedEvent.getState()) {
            try {
                this.init();
            } catch (Exception e) {
            }
        } else {
            log.info("watched other event: {}", watchedEvent);
        }
    }
}
