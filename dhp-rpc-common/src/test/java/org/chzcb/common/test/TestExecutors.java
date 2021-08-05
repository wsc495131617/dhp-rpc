package org.chzcb.common.test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TestExecutors {
    public static void main(String[] args) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(1);
        int i = 10;
        while (i-- > 0) {
            pool.execute(() -> {
                int y = 0;
                int x = 0;
                System.out.println(Thread.currentThread()+"ret:" + x + y);
            });
            Thread.sleep(100);
        }
    }
}
