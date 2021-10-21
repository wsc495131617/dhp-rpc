package org.dhp.thread;

/**
 * 分组的执行器
 * 同组的按顺序执行，不允许并发
 */
public interface GroupRunnable extends Runnable{
    int group();
}
