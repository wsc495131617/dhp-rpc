package org.dhp.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DebugUtils {
	static Logger logger = LoggerFactory.getLogger(DebugUtils.class);
	public static void printTrace() {
		try { throw new Exception(); } catch (Exception e) { logger.error("",e);}
	}
}
