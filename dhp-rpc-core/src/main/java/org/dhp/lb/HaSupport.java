package org.dhp.lb;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.dhp.common.utils.LocalIPUtils;
import org.dhp.core.spring.DhpProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * 1. 创建提交Prepare路径，或者更新Prepare路径
 * 2. 所有从接收到创建节点消息后，触发PREPARE_MASTER事件，开始本机进行数据准备
 * 3. 有从准备好数据，就调用HaSupport的preemptToMaster方法，进行抢占
 * 4. 抢占成功后，触发TOBE_MASTER，NodeCenter修改current node的状态
 */
@Slf4j
public class HaSupport implements Watcher {

    @Value("${dhp.lb.zk_url}")
    String zkUrl;

    @Value("${dhp.lb.name:defaultCluster}")
    String clusterName;

    @Resource
    DhpProperties dhpProperties;

    @Resource
    ApplicationContext applicationContext;

    protected ZooKeeper zk;

    //主节点路径
    protected String masterPath;
    protected String preparePath;

    /**
     * 节点名字
     */
    protected String nodeName;

    static int sessionTimeout = 3000;

    static ScheduledExecutorService pool = Executors.newScheduledThreadPool(1);

    @PostConstruct
    public void init() {
        try {
            connect();
            masterPath = "/" + clusterName + "_MASTER/" + dhpProperties.getName();
            preparePath = "/" + clusterName + "_PREPARE/" + dhpProperties.getName();
            master = false;
            state = "none";
            //给本节点设置一个全局唯一的名称
            nodeName = LocalIPUtils.hostName() + "_" + dhpProperties.getPort() + "_" + RandomStringUtils.random(8, true, true);
            log.info("HaSupport cur node: {}", nodeName);
        } catch (Exception e) {
            log.info("error: {}", e.getMessage(), e);
        }
    }

    /**
     * 状态，
     * none 待机
     * ready 准备中
     * working 工作中
     * giveUp 放弃了主
     */
    protected String state;

    /**
     * 是否主
     */
    @Getter
    protected boolean master;

    /**
     * 放弃主的身份后，需要主动发送
     *
     * @return
     */
    public boolean giveUpMaster() {
        // 如果已经是主，开始走放弃主的流程，先关闭当前节点状态为非主
        if ("working".equals(state)) {
            this.state = "giveUp";
            this.master = false;
            log.info("giveUpMaster {}", nodeName);
            return true;
        }
        return false;
    }

    /**
     * 主动清理
     */
    public void close() {
        //如果是主动放弃的，需要主动删除
        if (this.state.equals("giveUp")) {
            try {
                //删除masterPath
                if (zk.exists(masterPath, false) != null) {
                    //为了避免重复删除
                    zk.delete(masterPath, -1);
                }
            } catch (Exception e) {
            }
        }
    }

    public boolean hasMaster() {
        //首先查找是否已经有主了，有主就放弃申请，老老实实当个从
        try {
            Stat stat = zk.exists(masterPath, false);
            if (stat != null) {
                log.info("masterPath exists");
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 一般是第一个启动的，或者是主放弃了身份后，可以开始抢主了
     * Prepare节点没有就创建，有就设置当前节点内容
     *
     * @return
     */
    public boolean notifyToReady() {
        try {
            //如果当前是主，因为zk重启，或者网络断开之类，就直接
            if (this.state.equals("working")) {
                log.info("working");
                //直接调用成为master
                readyMaster();
                return true;
            }
            //直接尝试创建，只需要有一个从节点触发成功，所有节点都会收到创建Prepare的消息
            try {
                zk.create(preparePath, nodeName.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
            } catch (KeeperException e) {
                //如果节点已经存在
                if (e.code() == KeeperException.Code.NODEEXISTS) {
                    return false;
                } else {
                    log.error("preemptReady KeeperException: {}", e.getMessage(), e);
                    return false;
                }
            }
            return true;
        } catch (InterruptedException e) {
            log.error("preemptReady InterruptedException: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        if ("giveUp".equals(this.state)) {
            return;
        }
        log.info("WatchedEvent: {}", watchedEvent);
        if (Event.KeeperState.Expired == watchedEvent.getState()) {
            reconnect();
        } else if (Event.KeeperState.SyncConnected == watchedEvent.getState()) {
            //创建节点
            if (Event.EventType.NodeCreated == watchedEvent.getType()) {
                //如果主创建了，那么如果不是当前节点，需要恢复之前抢占的数据
                if (watchedEvent.getPath().equals(masterPath)) {
                    completeMaster();
                } else if (watchedEvent.getPath().equals(preparePath)) {
                    //从节点触发创建Prepare
                    //需要判断当前是否是主
                    readyMaster();
                }
            } else if (Event.EventType.NodeDeleted == watchedEvent.getType()) {
                //删除master分为
                // 1. 意外关闭 kill -9 等到zk的session时间到了后，会触发删除
                // 2. 自己giveUp
                // 3. 别人giveUp后收到消息
                if (watchedEvent.getPath().equals(masterPath)) {
                    //如果是自己放弃的，就不再抢主
                    if (this.state.equals("giveUp")) {
                        return;
                    }
                    this.notifyToReady();
                }
            }

        }
    }

    boolean connect() {
        try {
            zk = new ZooKeeper(zkUrl, sessionTimeout, this);
            String path = "/" + clusterName + "_MASTER";
            if (zk.exists(path, false) == null) {
                zk.create(path, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
            path = "/" + clusterName + "_PREPARE";
            if (zk.exists(path, false) == null) {
                zk.create(path, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
            zk.addWatch("/" + clusterName + "_MASTER", this, AddWatchMode.PERSISTENT_RECURSIVE);
            zk.addWatch("/" + clusterName + "_PREPARE", this, AddWatchMode.PERSISTENT_RECURSIVE);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    void reconnect() {
        //重连成功后
        if (connect()) {
            //重新走一遍抢的流程，正常来说，应该是当前节点抢成功
            this.notifyToReady();
        } else {
            pool.schedule(() -> {
                reconnect();
            }, 3, TimeUnit.SECONDS);
        }
    }

    void completeMaster() {
        try {
            byte[] data = zk.getData(masterPath, false, null);
            String curNodeName = new String(data);
            if (this.nodeName.equals(curNodeName)) {
                log.info("{} 竞争主成功", nodeName);
                this.applicationContext.publishEvent(new HaEvent(HaEvent.TOBE_MASTER, this));
            } else {
                log.info("{} 竞争主失败，cur={}", nodeName, curNodeName);
                this.state = "none";
                this.master = false;
                this.applicationContext.publishEvent(new HaEvent(HaEvent.PREEMPT_MASTER_FAILED, this));
            }
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    /**
     * 通知应用，可以去抢占主了
     */
    void readyMaster() {
        log.info("publishEvent HaEvent PREPARE_MASTER");
        this.applicationContext.publishEvent(new HaEvent(HaEvent.PREPARE_MASTER, this));
    }

    /**
     * 抢占准备，谁先抢占成功，内部就增加事件
     */
    public boolean preemptToMaster() {
        try {
            //抢占主之前需要删除PreparePath
            if (zk.exists(preparePath, false) != null) {
                //为了避免重复删除
                try {
                    zk.delete(preparePath, -1);
                } catch (Exception e) {
                }
            }
            Stat stat = zk.exists(masterPath, false);
            //如果有存在
            if (stat != null) {
                byte[] data = zk.getData(masterPath, false, null);
                String curNodeName = new String(data);
                log.info("preemptToMaster: cur={}, this={}, state={}", curNodeName, this.nodeName, state);
                if (curNodeName.equals(this.nodeName)) {
                    this.state = "working";
                    this.master = true;
                    log.info("当前节点自己重新抢占成功");
                    return true;
                } else {
                    this.applicationContext.publishEvent(new HaEvent(HaEvent.PREEMPT_MASTER_FAILED, this));
                }
            } else {
                try {
                    zk.create(masterPath, nodeName.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
                    log.info("create master path sucess {}", nodeName);
                } catch (KeeperException e) {
                    if (e.code() == KeeperException.Code.NODEEXISTS) {
                        log.warn("create master path exiteds {}", nodeName);
                        return false;
                    }
                }
                this.state = "working";
                this.master = true;
                log.info("抢占主成功：{}", nodeName);
            }
        } catch (KeeperException | InterruptedException e) {
            log.error("preemptReady error: {}", e.getMessage(), e);
        }
        return false;
    }

}
