package org.dhp.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class ProcessHookUtils {

	static Logger logger = LoggerFactory.getLogger(ProcessHookUtils.class);
	
	static {
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			public void run() {
				for(Process ps : allProcesses) {
					if(ps.isAlive()) {
						ps.destroyForcibly();
						logger.debug("Process {} Destoryed.", ps);
					}
				}
			}
		}));
	}
	
	static Set<Process> allProcesses = new HashSet<>();
	
	public static void addProcess(Process ps) {
		allProcesses.add(ps);
		logger.debug("Process {} Start.", ps);
	}
	
	public static void removeProcess(Process ps) {
		allProcesses.remove(ps);
		logger.debug("Process {} removed.", ps);
	}
	
	

}
