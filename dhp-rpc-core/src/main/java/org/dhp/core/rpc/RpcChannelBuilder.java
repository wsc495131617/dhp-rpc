package org.dhp.core.rpc;

public class RpcChannelBuilder {
    String name;
    int port;
    String host;
    long timeout;

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
        this.timeout = timeout;
        return this;
    }

    public RpcChannel build() {
        RpcChannel channel = new RpcChannel();
        return channel;
    }
}
