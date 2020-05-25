package org.dhp.core.rpc;

import java.io.IOException;

public interface IRpcServer {
    void start(RpcServerMethodManager methodManager) throws IOException;
    void shutdown();
}
