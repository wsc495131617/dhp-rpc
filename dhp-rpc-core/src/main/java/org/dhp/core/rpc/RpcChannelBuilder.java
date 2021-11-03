package org.dhp.core.rpc;

import org.dhp.net.grizzly.GrizzlyRpcChannel;
import org.dhp.net.netty4.NettyRpcChannel;
import org.dhp.net.nio.NioRpcChannel;
import org.dhp.net.zmq.ZmqRpcChannel;

/**
 * @author zhangcb
 */
public class RpcChannelBuilder {
    String name;
    int port;
    String host;
    long timeout = 5000;
    ChannelType type;

    public RpcChannelBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public RpcChannelBuilder setPort(int port) {
        this.port = port;
        return this;
    }

    public RpcChannelBuilder setHost(String host) {
        this.host = host;
        return this;
    }

    public RpcChannelBuilder setTimeout(long timeout) {
        if (timeout > 0) {
            this.timeout = timeout;
        }
        return this;
    }

    public RpcChannelBuilder setType(ChannelType type) {
        this.type = type;
        return this;
    }

    public RpcChannel build() {
        RpcChannel channel;
        if (this.type == ChannelType.Grizzly) {
            channel = new GrizzlyRpcChannel();
        } else if(this.type == ChannelType.NIO) {
            channel = new NioRpcChannel();
        } else if (this.type == ChannelType.Netty){
            channel = new NettyRpcChannel();
        } else if (this.type == ChannelType.ZMQ){
            channel = new ZmqRpcChannel();
        } else {
            throw new RpcException(RpcErrorCode.UNSUPPORTED_COMMAND_TYPE);
        }
        channel.setHost(this.host);
        channel.setPort(this.port);
        channel.setName(this.name);
        channel.setTimeout(this.timeout);
        return channel;
    }
}
