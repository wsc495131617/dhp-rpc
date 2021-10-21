package org.chzcb.common.test;

import lombok.SneakyThrows;
import org.dhp.thread.GroupRunnable;
import org.dhp.thread.GroupServiceExecutor;

import java.util.concurrent.Executors;

public class TestGroupExecutor {

    static class AddTask implements GroupRunnable {

        public int group() {
            return 0;
        }
        @SneakyThrows
        public void run() {
            System.out.println(System.currentTimeMillis()+" "+Thread.currentThread().getId()+" add");
//            Thread.sleep(1000);
        }
    }

    static class SubtractTask implements GroupRunnable {
        public int group() {
            return 2;
        }
        @SneakyThrows
        public void run() {
            System.out.println(System.currentTimeMillis()+" "+Thread.currentThread().getId()+" subtract");
//            Thread.sleep(1000);
        }
    }

    public static void main(String[] args) {
        GroupServiceExecutor executor = new GroupServiceExecutor(Executors.newFixedThreadPool(2));
        executor.execute(new AddTask());
        executor.execute(new AddTask());
        executor.execute(new SubtractTask());
        executor.execute(new SubtractTask());
        executor.execute(new AddTask());
        executor.execute(new SubtractTask());
        executor.execute(new AddTask());
        executor.execute(new SubtractTask());
    }
}
