package org.dhp.common.utils;

import java.util.Collection;


public class CollectionUtils {
	public static <T> void copyProperties(Collection sourceList, Collection<T> targetList, Class<T> cls) {
		for(Object obj : sourceList) {
			try {
				T target = cls.newInstance();
				BeansUtils.copyProperties(target, obj);
				targetList.add(target);
			} catch (Exception e) {
			}
		}
	}
}
