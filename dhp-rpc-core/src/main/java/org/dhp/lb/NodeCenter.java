package org.dhp.lb;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.*;
import org.dhp.common.utils.JacksonUtil;
import org.dhp.common.utils.LocalIPUtils;
import org.dhp.common.utils.SystemInfoUtils;
import org.dhp.core.rpc.Node;
import org.dhp.core.rpc.RpcErrorCode;
import org.dhp.core.rpc.RpcException;
import org.dhp.core.spring.DhpProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Node注册中心，如果开启注册中心，那么所有dhp的下游节点，都通过注册中心来查找
 */
@Slf4j
@Data
public class NodeCenter implements Watcher, ApplicationListener<ApplicationEvent> {

    /**
     * 集群名称
     */
    @Value("${dhp.lb.name:defaultCluster}")
    String clusterName;

    @Value("${dhp.lb.zk_url}")
    String zkUrl;

    /**
     * 集群模式
     * ms： 主备模式
     * cluster: 集群模式，默认集群
     */
    @Value("${dhp.lb.ha:cluster}")
    String ha = "cluster";

    @Resource
    DhpProperties dhpProperties;

    @Resource
    Environment environment;

    String currentPath;

    CountDownLatch connectedSemaphore;

    ZooKeeper zk;

    @Resource
    HaSupport haSupport;

    static int sessionTimeout = 3000;

    @PostConstruct
    public void init() throws Exception {
        connectedSemaphore = new CountDownLatch(1);
        if (zk != null) {
            zk.close();
            zk = null;
        }
        zk = new ZooKeeper(zkUrl, sessionTimeout, this);
        connectedSemaphore.await();
        if (dhpProperties.getPort() > 0) {
            //创建集群根目录
            String path = "/" + clusterName;
            if (zk.exists(path, false) == null) {
                createPath(path, new byte[0], CreateMode.PERSISTENT, "create cluster path");
            }
            //创建节点目录
            path = "/" + clusterName + "/" + dhpProperties.getName();
            if (zk.exists(path, false) == null) {
                createPath(path, new byte[0], CreateMode.PERSISTENT, "create node path");
            }

            //创建节点临时节点信息
            NodeStatus tmp = new NodeStatus();
            //本节点的host, 一般是本机局域网IP
            String host = LocalIPUtils.resolveIp();
            String hostName = LocalIPUtils.hostName();
            //当前完整路径
            currentPath = "/" + clusterName + "/" + dhpProperties.getName() + "/" + hostName + ":" + dhpProperties.getPort();
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
            tmp.setName(dhpProperties.getName());
            tmp.setPath(currentPath);
            tmp.setHost(host);
            tmp.setPort(dhpProperties.getPort());
            //集群模式每一个节点都是主
            if (ha.equals("cluster")) {
                tmp.setHaValue("master");
            } else {
                //先以从的身份启动，然后开始抢占主
                tmp.setHaValue("slave");
            }
            createPath(currentPath, JacksonUtil.bean2JsonBytes(tmp), CreateMode.EPHEMERAL, "create node");
            current = tmp;
        }
        //处理下游节点
        try {
            List<String> list = zk.getChildren("/" + clusterName, false);
            for (String path : list) {
                if (path.endsWith("MASTER")) {
                    continue;
                }
                List<String> subList = zk.getChildren("/" + clusterName + "/" + path, false);
                for (String subPath : subList) {
                    updateNextNode("/" + clusterName + "/" + path + "/" + subPath);
                }
            }
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        zk.addWatch("/" + clusterName, this, AddWatchMode.PERSISTENT_RECURSIVE);
    }


    NodeStatus current;

    /**
     * 5s更新一次节点信息
     */
    @Scheduled(fixedRate = 5000)
    public void updateNode() {
        if (current == null) {
            return;
        }
        current.setCpuLoad(SystemInfoUtils.getProcessCpuLoad() + SystemInfoUtils.getSystemCpuLoad());
        current.setMemLoad((double) SystemInfoUtils.getUsedMemory() / (double) SystemInfoUtils.getTotalMemorySize());
        current.setTotalLoad((current.getCpuLoad() + current.getMemLoad()) / 2);
        try {
            zk.setData(currentPath, JacksonUtil.bean2JsonBytes(current), -1);
        } catch (KeeperException e) {
            //如果连接已经断开，就重连
            if(e.code() == KeeperException.Code.CONNECTIONLOSS) {
                try {
                    init();
                } catch (Exception e1) {
                }
            }
            log.error("update Zk Error", e);
        } catch (InterruptedException e) {

        }
    }

    public void updateNextNode(String path) {
        //当前节点刷新内容，不处理
        if (path.equalsIgnoreCase(currentPath)) {
            return;
        }
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
                    //主才能为enable
                    boolean enable = nodeStatus.getHaValue().equals("master");
                    if (node.isEnable() != enable) {
                        node.setEnable(enable);
                        log.info("set node enable: {}", node);
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
                node.setEnable(nodeStatus.getHaValue().equals("master"));
                node.setPath(path);
                node.setTimeout(5000);
                nodes.add(node);
                log.info("add next node: {}", node);
            }
        } catch (Exception e) {
            log.error("updateNextNode: {} error", path, e);
        }
    }

    protected void createPath(String path, byte[] content, CreateMode mode, String context) {
        zk.create(path, content, ZooDefs.Ids.OPEN_ACL_UNSAFE, mode, new AsyncCallback.StringCallback() {
            @Override
            public void processResult(int i, String s, Object o, String s1) {
                if (i == 0) {
                    log.info("{} {}, {}", i, o, s);
                } else {
                    log.info("{},{},{},{}", KeeperException.Code.get(i), s, o, s1);
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
                // 如果是主从抢占路径的消息，就略过
                if (path.startsWith("/" + clusterName + "/MASTER")) {
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
                        log.info("delete node: {}", node);
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

    /**
     * 用于控制节点主从切换
     *
     * @param haEvent
     */
    @EventListener
    public void haEventListener(HaEvent haEvent) {
        if (haEvent.getEvent().equals(HaEvent.GIVE_UP_MASTER)) {
            //放弃后就设置为从
            current.setHaValue("slave");
        } else if (haEvent.getEvent().equals(HaEvent.TOBE_MASTER)) {
            current.setHaValue("master");
        } else {
            return;
        }
        try {
            zk.setData(currentPath, JacksonUtil.bean2JsonBytes(current), -1);
        } catch (KeeperException | InterruptedException e) {
            log.error("update Slave error: {}", e.getMessage(), e);
        }
    }

    public void close() {
        try {
            if (dhpProperties.getPort() > 0) {
                zk.delete(currentPath, -1);
                log.info("delete currentPath: {}", currentPath);
                current = null;
            }
        } catch (InterruptedException e) {
        } catch (KeeperException e) {
        }
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if(applicationEvent instanceof ContextClosedEvent) {
            this.close();
        }
    }
}
