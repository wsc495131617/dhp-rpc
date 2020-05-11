package org.dhp.core.rpc;

import org.dhp.core.grizzly.GrizzlyRpcChannel;
import org.dhp.core.netty4.NettyRpcChannel;

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
        if (timeout > 0)
            this.timeout = timeout;
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
        } else {
            channel = new NettyRpcChannel();
        }
        channel.setHost(this.host);
        channel.setPort(this.port);
        channel.setName(this.name);
        channel.setTimeout(this.timeout);
        channel.start();
        return channel;
    }
}
