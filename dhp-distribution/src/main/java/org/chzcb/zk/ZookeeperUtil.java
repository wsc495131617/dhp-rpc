package org.chzcb.zk;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Data
@ConfigurationProperties(prefix = "zookeeper")
public class ZookeeperUtil {
    String connectUrl;
    int connectTimeout = 300000;
    String rootPath = "/zk_locks";

    static Set<ZookeeperLock> locks = ConcurrentHashMap.newKeySet();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(()->{
            locks.forEach(ZookeeperLock::unlock);
        }));
    }

    public static void release(ZookeeperLock zookeeperLock) {
        locks.remove(zookeeperLock);
    }

    public ZookeeperLock createLock(String lockName) {
        try {
            ZookeeperLock lock = new ZookeeperLock(connectUrl, connectTimeout, rootPath, lockName);
            locks.add(lock);
            return lock;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }
}
