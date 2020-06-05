package org.dhp.core.rpc;

import lombok.extern.slf4j.Slf4j;
import org.dhp.common.rpc.Stream;
import org.dhp.common.rpc.StreamFuture;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zhangcb
 */
@Slf4j
public abstract class Session {
    protected Long id;
    public boolean isRegister() {
        return id != null;
    }
    public void setId(Long value) {
        this.id = value;
    }
    public Long getId() {
        return id;
    }
    
    public abstract void write(Message message);
    
    Set<Stream> streams = ConcurrentHashMap.newKeySet();
    
    Set<StreamFuture> futures = ConcurrentHashMap.newKeySet();
    
    public void addStream(Stream stream){
        streams.add(stream);
    }
    
    
    public void addFuture(StreamFuture future){
        futures.add(future);
    }
    
    public void destory(){
        streams.stream().forEach(stream -> {
            stream.onCanceled();
        });
        streams.clear();
        futures.stream().forEach(streamFuture -> {
            streamFuture.cancel(false);
        });
        futures.clear();
        log.info("create session {}", this);
    }
}
