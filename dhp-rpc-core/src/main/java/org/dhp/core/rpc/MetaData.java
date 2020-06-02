package org.dhp.core.rpc;

import java.util.HashMap;
import java.util.Map;

/**
 * @author zhangcb
 */
public class MetaData {
    Map<Integer, String> data = new HashMap<>();

    public void add(Integer key, String value) {
        data.put(key, value);
    }

    public String remove(Integer key) {
        return data.remove(key);
    }

    public Map<Integer, String> getData() {
        return data;
    }

    @Override
    public String toString() {
        return "MetaData{" +
                "data=" + data +
                '}';
    }
}
