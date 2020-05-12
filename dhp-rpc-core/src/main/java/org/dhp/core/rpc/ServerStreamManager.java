package org.dhp.core.rpc;

import org.dhp.common.rpc.Stream;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务端的StreamManager，用于管理所有的流
 */
public class ServerStreamManager {
    
    static Map<Integer, Stream> handlerMap = new ConcurrentHashMap<>();
    
    public Throwable dealThrowable(Message message) {
        return null;
    }
    
    public void setCompleteHandler(Integer id, Stream stream) {
        handlerMap.put(id, stream);
    }
    
    public void handleMessage(Message message){
    
    }
    
    
}
