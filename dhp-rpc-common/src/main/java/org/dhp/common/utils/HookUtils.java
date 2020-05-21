package org.dhp.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class HookUtils {
	
	static Logger logger = LoggerFactory.getLogger(HookUtils.class);
	
	static {
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			public void run() {
				for(Runnable task : allTasks) {
					try {
						task.run();						
					} catch (Exception e) {
					}
				}
			}
		}));
	}
	
	static Set<Runnable> allTasks = new HashSet<>();
	
	public static void addTask(Runnable task) {
		allTasks.add(task);
	}
	
	public static void removeTask(Runnable task) {
		allTasks.remove(task);
	}
}
