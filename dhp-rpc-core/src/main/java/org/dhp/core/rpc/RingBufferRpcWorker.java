package org.dhp.core.rpc;

import com.lmax.disruptor.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dhp.common.rpc.Stream;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 经测试性能很差，处理socket的消息不需要如此复杂，RingBuffer适用业务场景中转消息量非常大的场景，不适合IO
 */
@Slf4j
@Deprecated
public class RingBufferRpcWorker implements IRpcWorker {
    static final int LIMIT = 10;
    //最多65535条消息
    RingBuffer<RpcExecutor> allTasks = RingBuffer.createSingleProducer(new EventFactory<RpcExecutor>() {
        @Override
        public RpcExecutor newInstance() {
            return new RpcExecutor();
        }
    }, 1024, new YieldingWaitStrategy());

    final SequenceBarrier barrier = allTasks.newBarrier();

    protected long liveTime;

    @Getter
    String name;

    int id;

    @Getter
    long total = 0;

    public RingBufferRpcWorker(String name) {
        this.liveTime = System.currentTimeMillis();
        this.running = true;
        this.name = name;

    }

    public boolean isOld(long time) {
        return System.currentTimeMillis() - liveTime > time;
    }

    public int getSize() {
        return (int) allTasks.getCursor();
    }

    protected final Map<String, Double> commandCostList = new ConcurrentHashMap<>();

    public void execute(ServerCommand command, Stream stream, Message message, Session session) {
        long index = allTasks.next();
        RpcExecutor rpcExecutor = allTasks.get(index);
        rpcExecutor.setCommand(command);
        rpcExecutor.setSession(session);
        rpcExecutor.setMessage(message);
        rpcExecutor.setStream(stream);
        allTasks.publish(index);//发布，然后消费者可以读到
    }

    long lastWaking = System.nanoTime();
    volatile boolean running;

    public void dealTask(RpcExecutor task) {
        final Session session = task.session;
        final Message message = task.message;
        final long size = allTasks.getCursor();
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
            if (task != null) {
                task.release();
            }
        }
    }

    @Override
    public void run() {
        RpcExecutor task = null;
        long readIndex = Sequencer.INITIAL_CURSOR_VALUE;
        while (running) {
            try {
                long nextIndex = readIndex + 1;//当前读取到的指针+1，即下一个该读的位置
                long availableIndex = barrier.waitFor(nextIndex);//等待直到上面的位置可读取
                while (nextIndex <= availableIndex)//从下一个可读位置到目前能读到的位置(Batch!)
                {
                    task = allTasks.get(nextIndex);//获得Buffer中的对象
                    dealTask(task);
                    nextIndex++;
                }
                readIndex = availableIndex;//刷新当前读取到的位置
            } catch (Exception ex) {
                log.error("dealTask error", ex);
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
