package org.dhp.common.core;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

@Slf4j
public abstract class PoolWorker<T> extends BaseWorker<T> {

    ExecutorService pool;

    public PoolWorker(ExecutorService pool, BlockingQueue<T> queue, int maxSize) {
        super(queue, maxSize);
        this.pool = pool;
    }

    @Override
    public boolean addItem(T item) {
        if (!this.queue.offer(item)) {
            return false;
        }
        pool.execute(this);
        return true;
    }

    @Override
    public void run() {
        try {
            int batchSize = 10;
            List<T> list = new ArrayList<>(batchSize);
            queue.drainTo(list, batchSize);
            if (!list.isEmpty())
                dealItems(list);
        } catch (Throwable e) {
            log.error("PoolWorker dealItems error {}", e.getMessage(), e);
        }
    }
}
