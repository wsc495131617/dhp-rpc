package org.dhp.common.core;

import io.prometheus.client.Collector;
import io.prometheus.client.Gauge;
import lombok.Builder;
import lombok.Data;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

public class ThreadPoolExecutorMetrics extends Collector {
    static Gauge threadPoolGuage = Gauge.build(
            "thread_pool_guage",
            "线程池任务队列情况")
            .labelNames("name", "type")
            .register();

    static ThreadPoolExecutorMetrics instance;

    static {
        instance = new ThreadPoolExecutorMetrics();
        instance.register();
    }

    List<ThreadPoolTaskExecutorWrapper> threadPoolTaskExecutors = new ArrayList<>();

    List<ThreadPoolExecutorWrapper> threadPoolExecutors = new ArrayList<>();

    public static void addThreadPoolExecutor(ThreadPoolExecutor threadPoolExecutor, String name) {
        instance.threadPoolExecutors.add(ThreadPoolExecutorWrapper.builder()
                .threadPoolTaskExecutor(threadPoolExecutor)
                .name(name)
                .build());
    }

    public static void addThreadPoolTaskExecutor(ThreadPoolTaskExecutor threadPoolTaskExecutor, String name) {
        instance.threadPoolTaskExecutors.add(ThreadPoolTaskExecutorWrapper.builder()
                .threadPoolTaskExecutor(threadPoolTaskExecutor)
                .name(name)
                .build());
    }

    @Override
    public List<MetricFamilySamples> collect() {
        for(ThreadPoolTaskExecutorWrapper wrapper : threadPoolTaskExecutors) {
            threadPoolGuage.labels(wrapper.name, "completed").set((double)wrapper.threadPoolTaskExecutor.getThreadPoolExecutor().getCompletedTaskCount());
            threadPoolGuage.labels(wrapper.name, "active").set((double)wrapper.threadPoolTaskExecutor.getActiveCount());
            threadPoolGuage.labels(wrapper.name, "taskCount").set((double)wrapper.threadPoolTaskExecutor.getThreadPoolExecutor().getTaskCount());
            threadPoolGuage.labels(wrapper.name, "queued").set((double)wrapper.threadPoolTaskExecutor.getThreadPoolExecutor().getQueue().size());
            threadPoolGuage.labels(wrapper.name, "poolSize").set((double)wrapper.threadPoolTaskExecutor.getPoolSize());
            threadPoolGuage.labels(wrapper.name, "largestPoolSize").set((double)wrapper.threadPoolTaskExecutor.getThreadPoolExecutor().getLargestPoolSize());
        }
        for(ThreadPoolExecutorWrapper wrapper : threadPoolExecutors) {
            threadPoolGuage.labels(wrapper.name, "completed").set((double)wrapper.threadPoolTaskExecutor.getCompletedTaskCount());
            threadPoolGuage.labels(wrapper.name, "active").set((double)wrapper.threadPoolTaskExecutor.getActiveCount());
            threadPoolGuage.labels(wrapper.name, "taskCount").set((double)wrapper.threadPoolTaskExecutor.getTaskCount());
            threadPoolGuage.labels(wrapper.name, "queued").set((double)wrapper.threadPoolTaskExecutor.getQueue().size());
            threadPoolGuage.labels(wrapper.name, "poolSize").set((double)wrapper.threadPoolTaskExecutor.getPoolSize());
            threadPoolGuage.labels(wrapper.name, "largestPoolSize").set((double)wrapper.threadPoolTaskExecutor.getLargestPoolSize());
        }
        return new ArrayList(0);
    }

    @Data
    @Builder
    static class ThreadPoolTaskExecutorWrapper {
        ThreadPoolTaskExecutor threadPoolTaskExecutor;
        String name;
    }

    @Data
    @Builder
    static class ThreadPoolExecutorWrapper {
        ThreadPoolExecutor threadPoolTaskExecutor;
        String name;
    }
}
