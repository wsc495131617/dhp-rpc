package org.chzcb.common.test;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class TestStreamAndFor {
    public static void main(String[] args) throws InterruptedException {
        List<Integer> arr1 = new LinkedList<>();
        List<Integer> arr2 = new ArrayList<>();
        int MAX_COUNT = 10000;
        for (int i = 0; i < MAX_COUNT; i++) {
            arr1.add(i);
            arr2.add(i);
        }
        Thread.sleep(1000);
        AtomicLong count1 = new AtomicLong(0);
        long st = System.nanoTime();
        arr1.stream().forEach(id -> {
            count1.addAndGet(id);
        });
        System.out.println("linkedlist stream:"+(System.nanoTime() - st));
        AtomicLong count2 = new AtomicLong(0);
        st = System.nanoTime();
        for (int i= 0; i < MAX_COUNT; i++) {
            count2.addAndGet(arr1.get(i));
        }
        System.out.println("linkedlist    for:"+(System.nanoTime() - st));
        AtomicLong count3 = new AtomicLong(0);
        st = System.nanoTime();
        arr2.stream().forEach(id -> {
            count3.addAndGet(id);
        });
        System.out.println("arraylist  stream:"+(System.nanoTime() - st));
        AtomicLong count4 = new AtomicLong(0);
        st = System.nanoTime();
        for (int i= 0; i < MAX_COUNT; i++) {
            count4.addAndGet(arr2.get(i));
        }
        System.out.println("arraylist     for:"+(System.nanoTime() - st));
        AtomicLong count5 = new AtomicLong(0);
        st = System.nanoTime();
        for (int id : arr1) {
            count5.addAndGet(id);
        };
        System.out.println("linkedlist   for2:"+(System.nanoTime() - st));
        AtomicLong count6 = new AtomicLong(0);
        st = System.nanoTime();
        for (int id : arr2) {
            count6.addAndGet(id);
        }
        System.out.println("arraylist    for2:"+(System.nanoTime() - st));
    }
}
