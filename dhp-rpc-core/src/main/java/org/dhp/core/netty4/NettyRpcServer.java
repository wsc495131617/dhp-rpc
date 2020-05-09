package org.dhp.core.netty4;

import org.dhp.core.rpc.IRpcServer;
import org.dhp.core.rpc.RpcServerMethodManager;

public class NettyRpcServer implements IRpcServer {

    int port;

    public NettyRpcServer(int port) {
        this.port = port;
    }

    public void start(RpcServerMethodManager methodManager) {

    }
}
