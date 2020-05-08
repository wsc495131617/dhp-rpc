package org.dhp.core.netty4;

import org.dhp.core.rpc.IRpcServer;

public class NettyRpcServer implements IRpcServer {

    int port;

    public NettyRpcServer(int port) {
        this.port = port;
    }

    @Override
    public void start() {

    }
}
