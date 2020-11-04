package org.dhp.core.rpc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author zhangcb
 */
public class Workers {

    static final Logger logger = LoggerFactory.getLogger(Workers.class);

    /**
     * 特定线程池，用于单独执行某一个功能号，当某个任务比较耗时的时候，就通过线程池进行统一分流，为了不影响主流的任务流
     */
    public static int COMMAND_POOL_SIZE = 20;

    public static double COMMAND_MAX_RATE = 0.6;

    /**
     * 最大可接受的处理延迟，15秒，用于拒绝可能延迟过久的请求
     */
    public static double COMMAND_MAX_TIMEOUT = 15000;

    /**
     * 统一核心线程池，根据commandid%size进行分流
     */
    public static int CORE_POOL_SIZE = 8;
    /**
     * 通过属性路由的核心线程池
     */
    public static final int HASH_POOL_SIZE = 20;

    public static RpcExecutorService[] coreWorkers;

    public static final Map<String, RpcExecutorService> commandWorkers = new HashMap<>();

    /**
     * 100 毫秒的延迟 就需要单独分离Worker处理，避免影响主线程。
     */
    public static final double NEW_WORKER_THRESHOLD = 50;

    public static final double POOL_WORKER_THRESHOLD = 30;

    /**
     * 超过20个就用worker执行
     */
    public static final double TMP_ELASTIC_COUNT = 20;

    public static final BlockingQueue<Runnable> COMMAND_QUEUE = new LinkedBlockingQueue<>();

    /**
     * 纯计算，可以使用同步锁，粒度足够小
     * @param message
     * @return
     */
    public synchronized static RpcExecutorService getExecutorService(Message message) {
        if (coreWorkers == null) {
            coreWorkers = new RpcExecutorService[CORE_POOL_SIZE];
        }
        String commandId = message.getCommand();
        int index = message.getId() % CORE_POOL_SIZE;
        // 假如命令专有线程，那么就直接用
        if (commandWorkers.containsKey(commandId)) {
            RpcExecutorService worker = commandWorkers.get(commandId);
            // 首先worker的寿命要有1分钟吧，不然太浪费线程了,其次当前线程很空闲
            if (worker.isOld(60000) && (worker.getAvg(commandId) <= POOL_WORKER_THRESHOLD * 1000000
                    && worker.getSize() < TMP_ELASTIC_COUNT && COMMAND_QUEUE.isEmpty())) {
                worker.stop();
                logger.info("关闭：{}", worker);
                commandWorkers.remove(commandId);
            } else {
                return worker;
            }
        }

        if (coreWorkers[index] == null) {
            coreWorkers[index] = createWorker("RES_" + index);
        }

        RpcExecutorService worker = coreWorkers[index];
        // 核心线程如果出现某个功能号的延迟超过【新建Worker阈值】，那么久专门新建一个
        if (worker.getAvg(commandId) >= NEW_WORKER_THRESHOLD * 1000000
                || worker.getSize() > TMP_ELASTIC_COUNT) {
            Double costAvg = worker.getAvg(commandId);
            if (costAvg == null) {
                costAvg = NEW_WORKER_THRESHOLD * 1000000;
            }
            worker.setAvg(commandId, NEW_WORKER_THRESHOLD * 1000000 / 2);
            worker = createWorker("RES_CMD_" + commandId);
            worker.setAvg(commandId, costAvg);
            commandWorkers.put(commandId, worker);
            logger.info("创建新的{},commandId={}", worker, commandId);
        }
        return worker;
    }

    public static RpcExecutorService createWorker(String name) {
        RpcExecutorService worker = new RpcExecutorService();
        Thread t = new Thread(worker);
        t.setName(name);
        t.setDaemon(true);
        t.start();
        return worker;
    }
}
