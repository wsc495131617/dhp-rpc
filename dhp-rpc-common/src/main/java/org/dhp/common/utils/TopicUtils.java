package org.dhp.common.utils;

public class TopicUtils {
	public static boolean isContains(String express, String topic) {
		int len = topic.length();
		int checkLen = express.length();
		int checkIndex = 0;
		for(int i=0;i<len;i++) {
			char c1 = topic.charAt(i);
			if(checkIndex>=checkLen) {
				return false;
			}
			char c2 = express.charAt(checkIndex);
			if(c1 == '.' && c2 == '*') {
				if(++checkIndex>=checkLen) {
					return false;
				}
				c2 = express.charAt(checkIndex);
			}
			if(c2 == '?') {
				return true;
			}
			else if(c2 == '*') {
				continue;
			}
			if(c1 == c2) {
				checkIndex++;
				continue;
			}
			else
				return false;
		}
		if(checkIndex<checkLen-1) {
			return false;
		}
		return true;
	}
}
