package org.dhp.common.utils;

import org.dhp.common.sys.OSinfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

public class ProcessUtils {

	static Logger logger = LoggerFactory.getLogger(ProcessUtils.class);

	public static boolean isProcessRunning(int pid) {
		Runtime runtime = Runtime.getRuntime();
		Process process;
		try {
			if (OSinfo.isWindows()) {
				process = runtime.exec("cmd /c Tasklist");
				BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));

				String s;
				while ((s = in.readLine()) != null) {
					s = s.toLowerCase();
					if (s.contains(" " + pid + " ")) {
						return true;
					}
				}
			} else {
				process = runtime.exec("ps -ax");
				BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));

				String s;
				while ((s = in.readLine()) != null) {
					s = s.toLowerCase().trim();
					if (s.startsWith(pid + "")) {
						return true;
					}
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return false;
	}

	public static void forceKill(int pid) {
		kill(pid, true);
	}

	public static void kill(int pid, boolean isForce) {
		Runtime runtime = Runtime.getRuntime();
		try {
			if (OSinfo.isWindows()) {
				if (isForce)
					runtime.exec("taskkill /F /PID " + pid);
				else
					runtime.exec("taskkill /PID " + pid);
			} else {
				if (isForce)
					runtime.exec("kill -9 " + pid);
				else
					runtime.exec("kill " + pid);
			}
		} catch (Exception e) {
		}
	}

	public static List<String> exec(String cmd) {
		Runtime runtime = Runtime.getRuntime();
		Process process;
		List<String> ret = new LinkedList<>();
		try {
			if (OSinfo.isWindows()) {
				process = runtime.exec(cmd);
				BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));

				String s;
				while ((s = in.readLine()) != null) {
					ret.add(s);
				}
			} else {
				process = runtime.exec(cmd);
				BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));

				String s;
				while ((s = in.readLine()) != null) {
					ret.add(s);
				}
			}
		} catch ( Exception e) {
			logger.error(e.getMessage(), e);
		}
		return ret;
	}
}
