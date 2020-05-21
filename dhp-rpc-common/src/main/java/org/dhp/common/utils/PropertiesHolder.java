package org.dhp.common.utils;

import java.util.Properties;

public class PropertiesHolder {
	static Properties properties = new Properties();
	public static void addProperties(String filename) throws Exception {
		Properties p = PropertiesUtils.getProperties(filename);
		properties.putAll(p);
	}
	
	public static String getProperty(String key){
		return PropertiesUtils.findPropertyByName(properties, key);
	}
	
	public static String getProperty(String key, String defaultValue){
		String value = PropertiesUtils.findPropertyByName(properties, key);
		if(value == null)
			return defaultValue;
		return value;
	}
}
