package org.dhp.core.rpc;

import lombok.Data;

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

    public abstract void start();
    public abstract boolean connect() throws TimeoutException;

    public abstract Integer write(String name, byte[] argBody, Stream<byte[]> stream);
}
