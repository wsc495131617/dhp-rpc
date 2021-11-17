package org.dhp.common.core;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class BaseWorker<T> implements Runnable {

	BlockingQueue<T> queue;

	int maxSize = 1000;
	
	public BaseWorker(BlockingQueue<T> queue, int maxSize) {
		this.queue = queue;
		this.maxSize = maxSize;
	}

	public boolean addItem(T item) {
		if (!this.queue.offer(item)) {
			return false;
		}
		return this.queue.size() <= 1;
	}

	public void run() {
		while(!Thread.interrupted()) {
			try {
				List<T> list = new ArrayList<>();
				int i = 0;
				while (!queue.isEmpty()) {
					T item = queue.poll(100, TimeUnit.MILLISECONDS);
					if (item == null)
						break;
					if (++i > maxSize) {
						break;
					}
					list.add(item);
				}
				if (!list.isEmpty())
					dealItems(list);
				break;
			} catch (Throwable e) {
				log.error("BaseWorker error: {}", e.getMessage(), e);
			}
		}
	}

	abstract public void dealItems(List<T> list);
}
