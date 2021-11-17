package org.dhp.core.rpc;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dhp.common.rpc.Stream;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RpcWorker implements IRpcWorker {

    static final int LIMIT = 10;
    //最多20w条消息
    protected final LinkedBlockingQueue<RpcExecutor> allTasks = new LinkedBlockingQueue<>(Workers.MAX_WORKER_TASK_SIZE);

    protected long liveTime;

    @Getter
    protected String name;

    @Getter
    long total = 0;

    public RpcWorker(String name) {
        this.liveTime = System.currentTimeMillis();
        this.running = true;
        this.name = name;
    }

    public boolean isOld(long time) {
        return System.currentTimeMillis() - liveTime > time;
    }

    public int getSize() {
        return allTasks.size();
    }

    protected final Map<String, Double> commandCostList = new ConcurrentHashMap<>();

    public void execute(ServerCommand command, Stream stream, Message message, Session session) {
        try {
            allTasks.add(RpcExecutor.create(command, stream, message, session));
        } catch (InterruptedException e) {
            //超出队列运行的长度，熔断了
            throw new RpcException(RpcErrorCode.AUTH_ERROR);
        }
    }

    long lastWaking = System.nanoTime();
    boolean running;
    protected boolean dealing;


    public void dealTask(RpcExecutor task) {
        dealing = true;
        final Session session = task.session;
        final Message message = task.message;
        final int size = allTasks.size();
        try {
            long st = System.nanoTime();
            task.execute();
            total++;
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
                log.warn("处理：{}, 队列剩余：{}, 平均：{}ms， 当前：{}ms, {}", message.getCommand(), size, costAvg / 1000000, cost / 1000000, session);
                lastWaking = System.nanoTime();
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            dealing = false;
            if (task != null) {
                task.release();
            }
        }
    }

    @Override
    public void run() {
        //一直执行
        while (running || allTasks.size() > 0) {
            RpcExecutor task;
            try {
                task = allTasks.poll(100, TimeUnit.MILLISECONDS);
                if (task == null) {
                    lastWaking = System.nanoTime();
                    continue;
                }
            } catch (InterruptedException e1) {
                log.error(e1.getLocalizedMessage(), e1);
                continue;
            }
            dealTask(task);
        }
    }

    @Override
    public boolean isIdle() {
        return allTasks.isEmpty() && dealing == false;
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
