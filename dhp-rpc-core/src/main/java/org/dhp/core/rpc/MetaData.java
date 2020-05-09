package org.dhp.core.rpc;

import org.dhp.core.grizzly.GrizzlyOutputStream;

import java.util.HashMap;
import java.util.Map;

public class MetaData {
    Map<Integer, String> data = new HashMap<>();

    public void add(Integer key, String value) {
        data.put(key, value);
    }

    public String remove(Integer key) {
        return data.remove(key);
    }

    public int write(GrizzlyOutputStream outputStream) {
        int len = 1;
        outputStream.writeByte(data.size());
        for(Integer key : data.keySet()){
            outputStream.writeInt(key);
            len += 4;
            byte[] bytes = data.get(key).getBytes();
            outputStream.writeByte(bytes.length);
            len += 1;
            outputStream.writeBytes(bytes);
            len += bytes.length;
        }
        return len;
    }

    @Override
    public String toString() {
        return "MetaData{" +
                "data=" + data +
                '}';
    }
}
