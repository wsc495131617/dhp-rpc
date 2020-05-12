package org.dhp.core.rpc;

import org.dhp.common.rpc.Stream;
import org.dhp.core.spring.FrameworkException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务端的StreamManager，用于管理所有的流
 */
public class ClientStreamManager {
    
    static Map<Integer, Stream> handlerMap = new ConcurrentHashMap<>();
    
    public Throwable dealThrowable(Message message) {
        return null;
    }
    
    public void setStream(Integer id, Stream stream) {
        handlerMap.put(id, stream);
    }
    
    public void handleMessage(Message message){
        if (!handlerMap.containsKey(message.getId())) {
            return;
        }
        Stream handler = handlerMap.get(message.getId());
        MessageStatus status = message.getStatus();
        switch (status) {
            case Canceled:
                handler.onCanceled();
                break;
            case Completed:
                handler.onNext(message);
                handler.onCompleted();
                handlerMap.remove(message.getId());
                break;
            case Updating:
                handler.onNext(message);
                break;
            case Failed:
                handler.onFailed(dealThrowable(message));
                break;
            default:
                throw new FrameworkException("Sending MessageStatus can't response");
        }
    }
    
    
}
