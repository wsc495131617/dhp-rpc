package org.dhp.core.rpc;

import lombok.Data;
import org.dhp.common.rpc.Stream;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeoutException;

/**
 * Rpc 通道，用于发送信息
 */
@Data
public abstract class RpcChannel {
    String name;
    int port;
    String host;
    long timeout;
    ChannelType type;
    Long id;
    
    public RpcChannel(){
        id = System.currentTimeMillis()*10000000+ ThreadLocalRandom.current().nextInt(1000000,9999999);
    }

    public abstract void start();
    
    public abstract void register();

    public abstract void ping();

    public abstract boolean connect() throws TimeoutException;

    public abstract Integer write(String name, byte[] argBody, Stream<Message> stream);
}
