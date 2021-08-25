package org.dhp.core.rpc;

import io.prometheus.client.Gauge;
import org.dhp.common.utils.Cast;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author zhangcb
 */
public class Workers {

    static Gauge rpcExecutorGuage = Gauge.build(
            "rpc_executor_service_guage",
            "rpc executor service 任务队列情况")
            .labelNames("name", "type")
            .register();

    static final Logger logger = LoggerFactory.getLogger(Workers.class);

    /**
     * 统一核心线程池，根据commandid%size进行分流
     */
    public static int CORE_POOL_SIZE = 8;

    public static RpcExecutorService[] coreWorkers;

    public static final Map<String, RpcExecutorService> commandWorkers = new HashMap<>();

    /**
     * 100 毫秒的延迟 就需要单独分离Worker处理，避免影响主线程。
     */
    public static double NEW_WORKER_THRESHOLD = 50;

    /**
     * 超过20个就用worker执行
     */
    public static final double TMP_ELASTIC_COUNT = 20;

    static {
        //检查环境变量
        String value = System.getenv("dhp.rpc.pool.size");
        if (value != null) {
            CORE_POOL_SIZE = Cast.toInteger(value);
        }
        value = System.getenv("dhp.worker.threshold.new");
        if (value != null) {
            NEW_WORKER_THRESHOLD = Cast.toDouble(value);
        }
        coreWorkers = new RpcExecutorService[CORE_POOL_SIZE];
        Thread t = new Thread(() -> {
            while (true) {
                collect();
                try {
                    Thread.sleep(5000);
                } catch (Exception e) {
                }
            }
        });
        t.setName("RpcMetrics");
        t.setDaemon(true);
        t.start();
    }

    /**
     * 纯计算，可以使用同步锁，粒度足够小
     *
     * @param message
     * @return
     */
    public synchronized static RpcExecutorService getExecutorService(Message message) {
        String commandId = message.getCommand();
        int index = message.getId() % CORE_POOL_SIZE;
        // 假如命令专有线程，那么就直接用
        if (commandWorkers.containsKey(commandId)) {
            RpcExecutorService worker = commandWorkers.get(commandId);
            // 首先worker的寿命要有1分钟吧，不然太浪费线程了,其次当前线程很空闲
            if (worker.isOld(60000)) {
                worker.stop();
                logger.info("关闭：{}", worker);
                commandWorkers.remove(commandId);
                rpcExecutorGuage.remove(worker.getName(), "queue");
                rpcExecutorGuage.remove(worker.getName(), "total");
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
            worker = createWorker("RES_CMD_" + simpleCommandId(commandId));
            worker.setAvg(commandId, costAvg);
            commandWorkers.put(commandId, worker);
            logger.info("创建新的{},commandId={}", worker, commandId);
        }
        return worker;
    }

    private static String simpleCommandId(String commandId) {
        String[] arr = commandId.split("\\.");
        String simple = "";
        for (int i = arr.length - 1; i >= 0; i--) {
            if (simple.length() == 0) {
                simple = arr[i];
            } else {
                simple = arr[i].charAt(0) + "." + simple;
            }
        }
        return simple;
    }

    public static RpcExecutorService createWorker(String name) {
        RpcExecutorService worker = new RpcExecutorService(name);
        Thread t = new Thread(worker);
        t.setName(name);
        t.setDaemon(true);
        t.start();
        return worker;
    }

    static void collect() {
        for (RpcExecutorService rpcExecutorService : coreWorkers) {
            if(rpcExecutorService != null) {
                rpcExecutorGuage.labels(rpcExecutorService.getName(), "queue").set(rpcExecutorService.getSize());
                rpcExecutorGuage.labels(rpcExecutorService.getName(), "total").set(rpcExecutorService.getTotal());
            }
        }
        for (RpcExecutorService rpcExecutorService : commandWorkers.values()) {
            rpcExecutorGuage.labels(rpcExecutorService.getName(), "queue").set(rpcExecutorService.getSize());
            rpcExecutorGuage.labels(rpcExecutorService.getName(), "total").set(rpcExecutorService.getTotal());
        }
    }
}
