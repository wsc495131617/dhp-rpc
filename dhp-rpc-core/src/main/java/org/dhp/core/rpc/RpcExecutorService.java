package org.dhp.core.rpc;

import lombok.extern.slf4j.Slf4j;
import org.dhp.common.rpc.Stream;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RpcExecutorService implements Runnable {

    static final int LIMIT = 10;
    protected final LinkedBlockingQueue<RpcExecutor> allTasks = new LinkedBlockingQueue<>();
    protected long liveTime;

    public RpcExecutorService() {
        this.liveTime = System.currentTimeMillis();
        this.running = true;
    }

    public boolean isOld(long time) {
        return System.currentTimeMillis() - liveTime > time;
    }

    public int getSize() {
        return allTasks.size();
    }

    protected final Map<String, Double> commandCostList = new ConcurrentHashMap<>();

    public void execute(ServerCommand command, Stream stream, Message message, Session session) {
        allTasks.add(new RpcExecutor(command, stream, message, session));
    }

    long lastWaking = System.nanoTime();
    boolean running;

    @Override
    public void run() {
        log.info("start rpc executor service");
        //一直执行
        while (running || allTasks.size() > 0) {
            RpcExecutor task;
            int len = 0;
            try {
                task = allTasks.poll(100, TimeUnit.MILLISECONDS);
                if (task == null) {
                    lastWaking = System.nanoTime();
                    continue;
                }
                len = allTasks.size();
            } catch (InterruptedException e1) {
                log.error(e1.getLocalizedMessage(), e1);
                continue;
            }
            final Session session = task.session;
            final Message message = task.message;
            final int size = len;
            try {
                long st = System.nanoTime();
                task.execute();
                double cost = System.nanoTime() - st;
                Double costAvg;
                synchronized (commandCostList) {
                    String commandId = message.getCommand();
                    if (!commandCostList.containsKey(commandId)) {
                        costAvg = cost;
                        commandCostList.put(commandId, costAvg);
                    } else {
                        costAvg = commandCostList.get(commandId);
                    }
                    costAvg = cost * 2 / (LIMIT + 1) + costAvg * (LIMIT - 1) / (LIMIT + 1);
                    commandCostList.put(commandId, costAvg);
                }
                if (log.isInfoEnabled() && size > Workers.TMP_ELASTIC_COUNT
                        //如果100ms处理不了
                        && st + cost - lastWaking > 1000000 * 100
                ) {
                    log.info("处理：{}, 队列剩余：{}, 平均：{}ms， 当前：{}ms, {}", message.getCommand(), size, costAvg / 1000000, cost / 1000000, session);
                    lastWaking = System.nanoTime();
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    public void stop() {
        this.running = false;
    }

    public void setAvg(String commandId, Double costAvg) {
        this.commandCostList.put(commandId, costAvg);
    }

    public double getAvg(String commandId) {
        if (commandCostList.containsKey(commandId)) {
            return commandCostList.get(commandId);
        }
        return 0;
    }

}
