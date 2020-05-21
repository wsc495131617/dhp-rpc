package org.dhp.common.utils;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.util.Properties;
import java.util.ResourceBundle;


/**
 * 赋值工具
 * @author zhangcb
 * @date   2016年9月5日
 * @email  chzcb2008@gmail.com
 *
 */
public class PropertiesUtils {
	public static <T> T createFromResourceBundle(Class<T> cls, ResourceBundle bundle) throws InstantiationException, IllegalAccessException {
		T obj = cls.newInstance();
		Method[] methods = cls.getMethods();
		for(Method method : methods) {
			String methodName = method.getName();
			//找到set方法
			if(!methodName.startsWith("set")){
				continue;
			}
			String propertyName = methodName.substring(3, 4).toLowerCase()+methodName.substring(4);
			String value = findPropertyByName(bundle,propertyName);
			if(value == null) {
				continue;
			}
			BeansUtils.setProperty(obj, propertyName, value);
		}
		return obj;
	}
	
	public static String findPropertyByName(ResourceBundle bundle, String propertyName) {
		for(String key : bundle.keySet()) {
			int index = key.lastIndexOf(".");
			if(index>-1) {
				if(key.substring(index+1).equals(propertyName)) {
					return bundle.getString(key);
				}
			}
			else
			{
				if(key.equals(propertyName)) {
					return bundle.getString(key);
				}
			}
		}
		return null;
	}
	
	public static <T> T createFromProperties(Class<T> cls, Properties properties) throws InstantiationException, IllegalAccessException {
		T obj = cls.newInstance();
		Method[] methods = cls.getMethods();
		for(Method method : methods) {
			String methodName = method.getName();
			//找到set方法
			if(!methodName.startsWith("set")){
				continue;
			}
			String propertyName = methodName.substring(3, 4).toLowerCase()+methodName.substring(4);
			String value = findPropertyByName(properties,propertyName);
			if(value == null) {
				continue;
			}
			BeansUtils.setProperty(obj, propertyName, value);
		}
		return obj;
	}
	
	public static String findPropertyByName(Properties properties, String propertyName) {
		for(Object keyObject : properties.keySet()) {
			String key = keyObject.toString();
			int index = key.lastIndexOf(".");
			if(index>-1) {
				if(key.substring(index+1).equals(propertyName)) {
					return properties.getProperty(key);
				}
			}
			else
			{
				if(key.equals(propertyName)) {
					return properties.getProperty(key);
				}
			}
		}
		return null;
	}
	
	public static Properties getProperties(String fileName) throws Exception {
		Properties prop = new Properties();
		File file;
		if(PropertiesUtils.class.getResource("/") == null) {
			file = new File(fileName);
		}
		else
			file = new File(PropertiesUtils.class.getResource("/").getPath()+fileName);
		if(!file.exists()) {
			throw new Exception("文件找不到："+file.getAbsolutePath());
		}
		prop.load(new FileInputStream(file));
		return prop;
	}
	
}
