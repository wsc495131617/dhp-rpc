package org.dhp.common.utils;

import net.sf.cglib.beans.BeanCopier;
import net.sf.cglib.beans.BeanMap;
import net.sf.cglib.core.Converter;
import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class BeansUtils {

    static Logger logger = LoggerFactory.getLogger(BeansUtils.class);

    static Map<Class<?>, Map<Class<?>, BeanCopier>> cacheCopier = new ConcurrentHashMap<>();

    public static void copyObjectSkip(Object dest, Object origin) {
        copyObjectSkip(dest, origin, true);
    }

    public static void copyObjectSkip(Object dest, Object origin, boolean skipBlankOrZero) {
        if (origin == null) {
            return;
        }
        // 如果是目标是对象，那么
        if (dest instanceof Map) {
            if (origin instanceof Map) {
                Map map1 = (Map) origin;
                Map map2 = (Map) dest;
                map2.putAll(map1);
            } else {
                copyObjectToMap(origin, (Map) dest, skipBlankOrZero);
            }
        } else if (origin instanceof Map) {
            copyMapToObject((Map) origin, dest, skipBlankOrZero);
        } else {
            if (skipBlankOrZero) {

                BeanMap fromBeanMap = BeanMap.create(origin);
                BeanMap toBeanMap = BeanMap.create(dest);
                for (Object fromField : fromBeanMap.keySet()) {
                    if (!toBeanMap.containsKey(fromField))
                        continue;
                    if (!fromBeanMap.getPropertyType(fromField.toString())
                            .equals(toBeanMap.getPropertyType(fromField.toString()))) {
                        continue;
                    }
                    Object fromValue = fromBeanMap.get(fromField);
                    if (fromValue != null) {
                        toBeanMap.put(fromField, fromValue);
                    }
                }
            } else {
                BeanMap fromBeanMap = BeanMap.create(origin);
                BeanMap toBeanMap = BeanMap.create(dest);
                for (Object fromField : fromBeanMap.keySet()) {
                    if (!toBeanMap.containsKey(fromField))
                        continue;
                    Object fromValue = fromBeanMap.get(fromField);
                    // 如果两个类型一致，进行copy
                    if (fromBeanMap.getPropertyType(fromField.toString())
                            .equals(toBeanMap.getPropertyType(fromField.toString()))) {
                        toBeanMap.put(fromField, fromValue);
                    }
                }


                // BeanCopier beanCopier;
                // Class<?> fromCls = origin.getClass();
                // Class<?> toCls = dest.getClass();
                // Map<Class<?>, BeanCopier> map;
                // if (cacheCopier.containsKey(fromCls)) {
                // map = cacheCopier.get(fromCls);
                // } else {
                // map = new ConcurrentHashMap<>();
                // Map<Class<?>, BeanCopier> old = cacheCopier.putIfAbsent(fromCls, map);
                // if (old != null) {
                // map = old;
                // }
                // }
                // if (map.containsKey(toCls)) {
                // beanCopier = map.get(toCls);
                // } else {
                // beanCopier = BeanCopier.create(fromCls, toCls, true);
                // BeanCopier old = map.putIfAbsent(toCls, beanCopier);
                // if (old != null) {
                // beanCopier = old;
                // }
                // }
                // beanCopier.copy(origin, dest, converter);
            }
        }
    }

    private static void copyObjectToMap(Object origin, Map map, boolean skipBlankOrZero) {
        BeanMap beanMap = BeanMap.create(origin);
        for (Object key : beanMap.keySet()) {
            Object value = beanMap.get(key);
            if (Cast.isNotEmpty(value))
                map.put(key + "", value);
        }
    }

    private static void copyMapToObject(Map<String, Object> map, Object target,
                                        boolean skipBlankOrZero) {
        BeanMap beanMap = BeanMap.create(target);
        for (String key : map.keySet()) {
            if (beanMap.containsKey(key)) {
                Object value = map.get(key);
                if(skipBlankOrZero) {
                    if(Cast.isSupport(beanMap.getPropertyType(key))) {
                        beanMap.put(key, Cast.to(Cast.toString(value), beanMap.getPropertyType(key)));
                    } else {
                        beanMap.put(key, value);
                    }
                } else {
                    if(Cast.isSupport(beanMap.getPropertyType(key))) {
                        beanMap.put(key, Cast.to(Cast.toString(value), beanMap.getPropertyType(key)));
                    } else {
                        beanMap.put(key, value);
                    }
                }
            }
        }
    }

    public static void copyProperties(Object dest, Object origin) {
        // 如果是目标是对象，那么
        if (dest instanceof Map) {
            if (origin instanceof Map) {
                Map map1 = (Map) origin;
                Map map2 = (Map) dest;
                map2.putAll(map1);
            } else {
                copyObjectToMap(origin, (Map) dest);
            }
        } else if (origin instanceof Map) {
            copyMapToObject((Map) origin, dest);
        } else {
            BeanMap map1 = BeanMap.create(origin);
            BeanMap map2 = BeanMap.create(dest);
            for (Object field : map1.keySet()) {
                Object value = map1.get(field);
                if (map2.containsKey(field)) {
                    if(map1.getPropertyType(field.toString()).equals(map2.getPropertyType(field.toString()))) {
                        map2.put(field, value);
                    }
                }
            }

            // BeanCopier beanCopier;
            // Class<?> fromCls = origin.getClass();
            // Class<?> toCls = dest.getClass();
            // Map<Class<?>, BeanCopier> map;
            // if (cacheCopier.containsKey(fromCls)) {
            // map = cacheCopier.get(fromCls);
            // } else {
            // map = new ConcurrentHashMap<>();
            // Map<Class<?>, BeanCopier> old = cacheCopier.putIfAbsent(fromCls, map);
            // if (old != null) {
            // map = old;
            // }
            // }
            // if (map.containsKey(toCls)) {
            // beanCopier = map.get(toCls);
            // } else {
            // beanCopier = BeanCopier.create(fromCls, toCls, true);
            // BeanCopier old = map.putIfAbsent(toCls, beanCopier);
            // if (old != null) {
            // beanCopier = old;
            // }
            // }
            // beanCopier.copy(origin, dest, converter);
        }

    }

    public static void copyMapToObject(Map<String, Object> map, Object target) {
        BeanMap beanMap = BeanMap.create(target);
        for (String key : map.keySet()) {
            if (beanMap.containsKey(key)) {
                Class type = beanMap.getPropertyType(key);
                if (Cast.isSupport(type)) {
                    beanMap.put(key, Cast.to(Cast.toString(map.get(key)), type));
                } else {
                    Object value = map.get(key);
                    if (value.getClass() == type) {
                        beanMap.put(key, value);
                    } else {
                        logger.warn("跳过属性复制：{}", key);
                    }
                }
            }
        }
    }

    public static void copyObjectToMap(Object target, Map<String, Object> map) {
        BeanMap beanMap = BeanMap.create(target);
        for (Object key : beanMap.keySet()) {
            map.put(key + "", beanMap.get(key));
        }
    }

    static CastConverter converter = new CastConverter();

    static CastSkipConverter skipConverter = new CastSkipConverter();

    public static class CastConverter implements Converter {
        public Object convert(Object value, Class target, Object context) {
            if (Cast.isSupport(target))
                return Cast.to(Cast.toString(value), target);
            return value;
        }
    }

    public static class CastSkipConverter implements Converter {
        public Object convert(Object value, Class target, Object context) {
            if (Cast.isNotEmpty(value)) {
                return Cast.to(Cast.toString(value), target);
            }
            return null;
        }
    }

    /**
     * 把orig的属性复制到dest
     * 
     * @param dest
     * @param orig
     */
    public static void copyObject(Object dest, Object orig) {
        try {
            copyProperties(dest, orig);
        } catch (Exception e) {
        }
    }

    /**
     * 把destList的对象复制到origList的对象里面
     * 
     * @param destList
     * @param origList
     * @param cls
     */
    public static <T> void copyList(List<T> destList, List origList, Class<T> cls) {
        if (origList == null)
            return;
        for (Object orig : origList) {
            T t;
            try {
                t = cls.newInstance();
                copyObject(t, orig);
                destList.add(t);
            } catch (InstantiationException e) {
            } catch (IllegalAccessException e) {
            }
        }
    }

    public static Object getDepthProperties(Object bean, String name) {
        String[] keys = name.split("\\.");
        Object value = bean;
        for (String key : keys) {
            value = getProperty(bean, key);
        }
        return value;
    }

    public static Object getProperty(Object bean, String param) {
        BeanMap beanMap = BeanMap.create(bean);
        return beanMap.get(param);
    }

    public static Object[] getProperties(Object bean, String[] params) {
        return getProperties(bean, params, null);
    }

    public static Map<String, Object> pick(Object bean, Set<String> fields) {
        Map<String, Object> map = new HashMap<>();
        for (String key : fields) {
            map.put(key, getProperty(bean, key));
        }
        return map;
    }

    public static Object[] getProperties(Object bean, String[] params, String defaultValue) {
        int len = params.length;
        Object[] values = new Object[len];
        for (int i = 0; i < len; i++) {
            String param = params[i];
            Object value;
            value = BeansUtils.getProperty(bean, param);
            values[i] = value;
        }
        return values;
    }



    public static void setProperty(Object bean, String key, String value) {

        try {
            BeanUtils.setProperty(bean, key, value);
        } catch (IllegalAccessException e) {
        } catch (InvocationTargetException e) {
        }

        // BeanMap beanMap = BeanMap.create(bean);
        // Class<?> type = beanMap.getPropertyType(key);
        // beanMap.put(key, Cast.to(value, type));
    }

    public static void setProperty(Object bean, String key, Object value) {
        // BeanMap beanMap = BeanMap.create(bean);
        // Class<?> type = beanMap.getPropertyType(key);
        // beanMap.put(key, Cast.to(Cast.toString(value), type));
        try {
            BeanUtils.setProperty(bean, key, value);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public static boolean equals(Object origin, Object dest) {
        if (origin == null) {
            return (dest == null);
        }
        return origin.equals(dest);
    }
}
