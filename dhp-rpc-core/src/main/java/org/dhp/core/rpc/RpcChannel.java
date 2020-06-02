package org.dhp.core.rpc;

import lombok.Data;
import org.dhp.common.rpc.Stream;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeoutException;

/**
 * Rpc 通道，用于发送信息
 * @author zhangcb
 */
@Data
public abstract class RpcChannel {
    String name;
    int port;
    String host;
    long timeout;
    ChannelType type;
    Long id;
    
    //等待关闭的连接，有可能是网络延迟，或者服务端准备关闭的
    protected Set<Object> readyToCloseConns = ConcurrentHashMap.newKeySet();
    
    public RpcChannel(){
        id = System.currentTimeMillis()*10000000+ ThreadLocalRandom.current().nextInt(1000000,9999999);
    }
    
    /**
     * start channel
     */
    public abstract void start();
    
    /**
     * ping channel with connected
     */
    public abstract void ping();
    
    /**
     * connect to server
     * @return
     * @throws TimeoutException
     */
    public abstract boolean connect() throws TimeoutException;
    
    /**
     * write message to server
     * @param name
     * @param argBody
     * @param stream
     * @return
     */
    public abstract Integer write(String name, byte[] argBody, Stream<Message> stream);
}
