package org.dhp.core.rpc;


import lombok.extern.slf4j.Slf4j;
import org.dhp.common.rpc.Stream;

import java.util.concurrent.ExecutorService;

/**
 * 共用线程池来处理消息
 */
@Slf4j
public class PooledRpcWorker extends RpcWorker {
    ExecutorService pool;

    public PooledRpcWorker(String name, ExecutorService pool) {
        super(name);
        this.pool = pool;
    }

    @Override
    public void execute(ServerCommand command, Stream stream, Message message, Session session) {
        try {
            allTasks.add(RpcExecutor.create(command, stream, message, session));
        } catch (InterruptedException e) {
            //超出队列运行的长度，熔断了
            throw new RpcException(RpcErrorCode.AUTH_ERROR);
        }
        pool.execute(this);
    }

    @Override
    public void run() {
        //一直执行
        RpcExecutor task = null;
        try {
            task = allTasks.take();
            if (task == null) {
                lastWaking = System.nanoTime();
                return;
            }
        } catch (InterruptedException e1) {
            log.error(e1.getLocalizedMessage(), e1);
            return;
        }
        dealTask(task);
    }
}
