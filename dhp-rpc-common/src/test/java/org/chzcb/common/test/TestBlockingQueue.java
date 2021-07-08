package org.chzcb.common.test;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class TestBlockingQueue {
    public static void main(String[] args) {
        LinkedBlockingQueue<Long> queue = new LinkedBlockingQueue<>();
        Thread t1 = new Thread(()->{
            long id = 0;
            while(id++<10000) {
                queue.add(id);
                if(id %100 == 0){
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        t1.start();
        
        while(true) {
            try {
                Long id = queue.poll(10, TimeUnit.MILLISECONDS);
                if(id == null) {
                    continue;
                }
                List<Long> idList = new LinkedList<>();
                idList.add(id);
                queue.drainTo(idList, 100);
                System.out.println(idList.size()+":"+idList);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
