package org.dhp.common.utils;

import org.apache.commons.lang3.StringUtils;

/**
 * 打印消息的工具
 * @author zhangcb
 * @date   2017年2月16日
 * @email  chzcb2008@gmail.com
 *
 */
public class PrintUtils {
	
	public static void echo(Object obj) {
		System.out.println(obj.toString());
	}
	
	public static void echo(byte[] arr,int len) {
		StringBuffer sb = new StringBuffer();
		sb.append(len+"[");
		if(arr != null && arr.length>=len) {
			for(int i=0;i<len;i++) {
				byte item = arr[i];
				sb.append(StringUtils.leftPad(Integer.toHexString(item&0xFF),2,'0'));
				sb.append(',');
			}
			sb.setCharAt(sb.length()-1, ']');
		}
		else
			sb.append(']');
		System.out.println(sb.toString());
	}
	
	public static void echo(byte[] arr) {
		echo(arr,arr.length);
	}
	
	public static void echoByteValue(byte[] arr,int len) {
		StringBuffer sb = new StringBuffer();
		sb.append(len+"[");
		if(arr != null && arr.length>=len) {
			for(int i=0;i<len;i++) {
				byte item = arr[i];
				sb.append(item);
				sb.append(',');
			}
			sb.setCharAt(sb.length()-1, ']');
		}
		else
			sb.append(']');
		System.out.println(sb.toString());
	}
	
	public static void echo(Object[] arr) {
		StringBuffer sb = new StringBuffer();
		sb.append('[');
		if(arr != null && arr.length>0) {
			for(Object item:arr) {
				if(item != null)
					sb.append(item);
				sb.append(',');
			}
			sb.setCharAt(sb.length()-1, ']');
		}
		else
			sb.append(']');
		System.out.println(sb.toString());
	}
}
