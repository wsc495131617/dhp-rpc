package org.dhp.core.rpc;

import java.util.HashMap;
import java.util.Map;

/**
 * @author zhangcb
 */
public class MetaData {
    Map<Integer, String> data = new HashMap<>();

    public MetaData add(Integer key, String value) {
        data.put(key, value);
        return this;
    }

    public String remove(Integer key) {
        return data.remove(key);
    }

    public Map<Integer, String> getData() {
        return data;
    }

    public void clear() {
        if(this.data!=null) {
            this.data.clear();
            this.data = null;
        }
    }

    @Override
    public String toString() {
        return "MetaData{" +
                "data=" + data +
                '}';
    }
}
