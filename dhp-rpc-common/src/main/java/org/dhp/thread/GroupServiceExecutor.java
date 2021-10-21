package org.dhp.thread;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

public class GroupServiceExecutor<R extends GroupRunnable> {

    ExecutorService pool;

    Map<Integer, GroupWorker> groupWorkerMap = new ConcurrentHashMap<>();

    public GroupServiceExecutor(ExecutorService pool) {
        this.pool = pool;
    }

    final GroupWorker getWorker(int group) {
        if (groupWorkerMap.containsKey(group)) {
            return groupWorkerMap.get(group);
        }
        GroupWorker groupWorker = new GroupWorker();
        GroupWorker old = groupWorkerMap.putIfAbsent(group, groupWorker);
        if (old != null) {
            groupWorker = old;
        }
        return groupWorker;
    }

    public void execute(R task) {
        GroupWorker worker = getWorker(task.group());
        if (worker.addTask(task) > 0) {
            pool.execute(worker);
        }
    }

    static class GroupWorker<R extends GroupRunnable> implements Runnable {
        LinkedBlockingQueue<R> queue = new LinkedBlockingQueue<>();

        public int addTask(R task) {
            queue.add(task);
            return queue.size();
        }

        public void run() {
            List<R> taskList = new ArrayList<>(10);
            queue.drainTo(taskList, 10);
            if (taskList.isEmpty()) {
                return;
            }
            for (R task : taskList) {
                task.run();
            }
        }
    }
}
