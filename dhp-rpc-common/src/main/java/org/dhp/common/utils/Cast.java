package org.dhp.common.utils;

import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;

public class Cast {
    public static int toInteger(Object str) {
        if (str == null)
            return 0;
        if (str instanceof Number)
            return ((Number) str).intValue();
        else if (str instanceof Integer)
            return (Integer) str;
        return toInteger(str.toString());
    }

    public static byte toByte(Object obj) {
        if (obj == null)
            return 0;
        return (byte) obj;
    }

    public static boolean toBoolean(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof Boolean)
            return (boolean) obj;
        if (obj.equals("false"))
            return false;
        else
            return true;
    }

    public static double toDouble(Object number) {
        if (number == null)
            return 0L;
        if (number instanceof Number)
            return ((Number) number).doubleValue();
        else if (number instanceof String) {
            final String str = (String) number;
            if (isNumeric(str) > 0)
                return Double.valueOf(str);
            else
                return 0L;
        } else
            return 0L;
    }

    public static long toLong(Object number) {
        if (number == null)
            return 0L;
        if (number instanceof Number)
            return ((Number) number).longValue();
        else if (number instanceof String) {
            if (org.apache.commons.lang3.StringUtils.isNoneBlank((String) number))
                return Long.valueOf((String) number);
            else
                return 0L;
        } else
            return 0L;
    }

    public static int toInteger(String str) {
        if (str == null)
            return 0;
        str = str.trim();
        if (str.length() == 0)
            return 0;
        int i = isNumeric(str);
        if (i == 1)
            return Integer.parseInt(str);
        else if (i == 2)
            return Double.valueOf(str).intValue();
        else
            return 0;
    }

    public static String toString(Object obj) {
        if (obj == null) {
            return null;
        }
        return obj.toString();
    }

    /**
     * 转化成BigDecimal
     * 
     * @param obj
     * @return
     */
    public static BigDecimal toBigDecimal(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof BigDecimal) {
            return (BigDecimal) obj;
        } else {
            try {
                BigDecimal bigDecimal = new BigDecimal(obj.toString());
                return bigDecimal;
            } catch (Exception e) {
                return null;
            }

        }
    }

    /**
     * 是否为数字
     * 
     * @param str
     * @return
     */
    public static int isNumeric(String str) {
        if (str == null)
            return 0;
        boolean isdouble = false;
        for (int i = str.length(); --i >= 0;) {
            char c = str.charAt(i);
            if (i == 0 && c == '-') {
                continue;
            } else if (c == '.') {
                if (isdouble) {
                    return 0;
                }
                isdouble = true;
                continue;
            } else if (!Character.isDigit(str.charAt(i))) {
                return 0;
            }
        }
        if (isdouble)
            return 2;
        return 1;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static HashMap copyMap(HashMap map) {
        HashMap newmap = new HashMap();
        for (Object key : map.keySet()) {
            newmap.put(key, map.get(key));
        }
        return newmap;
    }

    public static void destory(Hashtable map) {
        for (Iterator keyit = map.keySet().iterator(); keyit.hasNext();) {
            Object key = keyit.next();
            Object value = map.get(key);
            if (value instanceof HashMap) {
                HashMap valuemap = (HashMap) value;
                destory(valuemap);
            }
            keyit.remove();
        }
    }

    @SuppressWarnings("rawtypes")
    public static void destory(HashMap map) {
        for (Iterator keyit = map.keySet().iterator(); keyit.hasNext();) {
            Object key = keyit.next();
            Object value = map.get(key);
            if (value instanceof HashMap) {
                HashMap valuemap = (HashMap) value;
                destory(valuemap);
            }
            keyit.remove();
        }
    }

    public static String objectToString(Object obj) {
        if (obj.getClass().equals(String.class)) {
            return obj.toString();
        } else {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                ObjectOutputStream oos = new ObjectOutputStream(out);
                oos.writeObject(obj);
                byte[] bytes = out.toByteArray();
                return new String(bytes, "ISO-8859-1");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    public static Object stringToObject(String string) {
        try {
            byte[] bytes = string.getBytes("ISO-8859-1");
            ByteArrayInputStream in = new ByteArrayInputStream(bytes);
            ObjectInputStream ois;
            ois = new ObjectInputStream(in);
            return ois.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static int bytesToInt(byte[] bytes) {
        int length = 4;
        int intValue = 0;
        for (int i = length - 1; i >= 0; i--) {
            int offset = i * 8; // 24, 16, 8
            intValue |= (bytes[i] & 0xFF) << offset;
        }
        return intValue;
    }

    public static Object bytesToObject(byte[] bytes) {
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(bytes);
            ObjectInputStream ois;
            ois = new ObjectInputStream(in);
            return ois.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected final static ByteArrayOutputStream out = new ByteArrayOutputStream();
    protected static ObjectOutputStream oos;

    public static byte[] objectToBytes(Object obj) throws IOException {
        out.reset();
        try {
            if (oos == null)
                oos = new ObjectOutputStream(out);
            else {
                oos.reset();
            }
            oos.writeObject(obj);
            byte[] bytes = out.toByteArray();
            return bytes;
        } finally {
            out.close();
        }
    }

    public static byte[] stringToBytes(String str) {
        StringBuffer sb = new StringBuffer(str);
        char c = sb.charAt(0);
        ByteBuffer buffer = ByteBuffer.allocate(sb.length() * 2);
        int index = 0;
        while (index < sb.length()) {
            buffer.putChar(sb.charAt(index++));
        }
        return buffer.array();
    }

    public static String bytesToString(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        StringBuffer sb = new StringBuffer();
        while (buffer.hasRemaining()) {
            sb.append(buffer.getChar());
        }
        return sb.toString();
    }

    public static long combineInt2Long(int low, int high) {
        return (low & 0xFFFFFFFFl) | (((long) high << 32) & 0xFFFFFFFF00000000l);
    }

    public static int[] separateLong2int(Long val) {
        int[] ret = new int[2];
        ret[0] = (int) (0xFFFFFFFFl & val);
        ret[1] = (int) ((0xFFFFFFFF00000000l & val) >> 32);
        return ret;
    }

    public static int getLongLowInt(Long val) {
        if (val == null)
            return 0;
        return (int) (0xFFFFFFFFl & val);
    }

    public static int getLongHighInt(Long val) {
        if (val == null)
            return 0;
        return (int) ((0xFFFFFFFF00000000l & val) >> 32);
    }

    public static boolean isIntInList(int i, int[] list) {
        for (int index = 0; index < list.length; index++) {
            if (list[index] == i)
                return true;
        }
        return false;
    }

    public static int[] stringToInts(String str, String regex) {
        String[] arr = str.split(regex);
        final int length = arr.length;
        int[] ret = new int[length];
        for (int i = 0; i < length; i++) {
            ret[i] = Cast.toInteger(arr[i]);
        }
        return ret;
    }

    /**
     * Convert byte[] to hex string.这里我们可以将byte转换成int，然后利用Integer.toHexString(int)来转换成16进制字符串。
     * 
     * @param src byte[] data
     * @return hex string
     */
    public static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

    /**
     * Convert hex string to byte[]
     * 
     * @param hexString the hex string
     * @return byte[]
     */
    public static byte[] hexStringToBytes(String hexString) {
        if (hexString == null || hexString.equals("")) {
            return null;
        }
        hexString = hexString.toUpperCase();
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }
        return d;
    }


    public static double strToDouble(String str) {
        if (str == null || str.isEmpty())
            return 0.0;
        int len = str.length();
        int p = str.indexOf('%');
        if (p == len - 1) {
            return Double.valueOf(str.substring(0, len - 1)) / 100;
        } else if (p > -1)
            return 0.0;
        if (str.equals("true"))
            return 1;
        else
            return toDouble(str);
    }

    /**
     * Convert char to byte
     * 
     * @param c char
     * @return byte
     */
    private static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }

    // protected static String[] stringlist = new
    // String[]{"0","1","2","3","4","5","6","7","8","9","10","11","12","13","14","15","16","17","18","19","20","21","22","23","24","25","26","27","28","29","30"};
    //
    // protected static ConcurrentHashMap<Integer, String> strMap = new ConcurrentHashMap<Integer,
    // String>();

    public static String cacheString(int i) {
        /*
         * if(i<=30 && i>=0){ return stringlist[i]; } else { if(strMap.containsKey(i)){ return
         * strMap.get(i); } else { String str = String.valueOf(i); strMap.put(i, str); return str; }
         * }
         */
        return String.valueOf(i);
    }

    public static boolean isNotEmpty(Object value) {
        if (value == null)
            return false;
        Class<?> type = value.getClass();
        if (type == int.class || type == Integer.class) {
            return toInteger(value) != 0;
        } else if (type == long.class || type == Long.class) {
            return toLong(value) != 0;
        } else if (type == BigDecimal.class) {
            return !toBigDecimal(value).equals(BigDecimal.ZERO);
        } else if (type == String.class) {
            return StringUtils.isNoneBlank(value.toString());
        } else if (type == double.class || type == Double.class) {
            return toDouble(value) != 0.0;
        }
        return true;
    }

    public static Object to(String value, Class<?> type) {
        if (type == int.class || type == Integer.class) {
            return toInteger(value);
        } else if (type == long.class || type == Long.class) {
            return toLong(value);
        } else if (type == BigDecimal.class) {
            return toBigDecimal(value);
        } else if (type == String.class) {
            return value;
        } else if (type == double.class || type == Double.class) {
            return toDouble(value);
        } else if (type == boolean.class || type == Boolean.class) {
            return toBoolean(value);
        }
        return null;
    }

    public static boolean isSupport(Object obj) {
        return isSupport(obj.getClass());
    }

    public static boolean isSupport(Class type) {
        if (type == int.class || type == Integer.class || type == long.class || type == Long.class
                || type == BigDecimal.class || type == String.class || type == double.class
                || type == Double.class || type == boolean.class || type == Boolean.class) {
            return true;
        }
        return false;
    }

    public static Object cast(String value, Class<?> type) {
        return to(value, type);
    }
}
