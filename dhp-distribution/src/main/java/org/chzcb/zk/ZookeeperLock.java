package org.chzcb.zk;

import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * 通过Zookeeper支持的阻塞锁
 */
@Slf4j
public class ZookeeperLock implements Watcher, Lock {
    private ZooKeeper zk = null;
    private String rootLockName = "/locks";
    private CountDownLatch waitCountDownLatch;
    private String curName;
    private String waitName;
    private String lockName;

    public ZookeeperLock(String connectUrl, int zkTimeOut, String rootLockName, String lockName) throws Exception {
        try {
            this.rootLockName = rootLockName;
            zk = new ZooKeeper(connectUrl, zkTimeOut, this);
            log.info("create zookeeper: sessionId={}, state={}", zk.getSessionId(), zk.getState());
            this.lockName = lockName;
            Stat stat = zk.exists(rootLockName, false);
            if (stat == null) {
                zk.create(rootLockName, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            } else {
                log.info("root node existed!");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void process(WatchedEvent event) {
        log.info("process: {}", event);
        if(this.waitCountDownLatch != null) {
            this.waitCountDownLatch.countDown();
        }
    }

    public void lock() {
        if (tryLock()) {
            return;
        } else {
            waitForLock();
        }
    }

    private void waitForLock() {
        try {
            log.info("waiting for {}", waitName);
            waitCountDownLatch = new CountDownLatch(1);
            waitCountDownLatch.await();
            waitCountDownLatch = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void lockInterruptibly() throws InterruptedException {
    }

    public boolean tryLock() {
        try {
            curName = zk.create(rootLockName+"/"+lockName, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE,
                    CreateMode.EPHEMERAL_SEQUENTIAL);
            //获取子顺序节点，不设置监视器
            List<String> children = zk.getChildren(rootLockName, false);
            //排序
            Collections.sort(children);
            //判断当前节点是否为最小节点
            int curIndex = Collections.binarySearch(children, curName.substring(curName.lastIndexOf("/")+1));
            if(curIndex == 0) {
                return true;
            }
            //获取上一个
            String prev = children.get(curIndex-1);
            this.waitName = prev;
            //监听上一个节点
            Stat stat = zk.exists(rootLockName+"/"+prev, true);
            //加点是否存在，基本上来说是存在的，但是万一当前节点创建和监听中间，上一个正好释放，那就GG了
            if(stat == null) {
                return true;
            }
            return false;
        } catch (KeeperException e) {
            log.error(e.getMessage(), e);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }

    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return false;
    }


    public void unlock() {
        try {
            log.info("unlock {}", curName);
            zk.delete(curName,0);
            zk.close();
            ZookeeperUtils.release(this);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        } catch (KeeperException e) {
            log.error(e.getMessage(), e);
        }
    }

    public Condition newCondition() {
        return null;
    }



}
