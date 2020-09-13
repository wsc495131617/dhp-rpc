package org.dhp.core.rpc;

import java.io.IOException;

/**
 * @author zhangcb
 */
public interface IRpcServer {
    /**
     * start server
     * @param methodManager
     * @throws IOException
     */
    void start(RpcServerMethodManager methodManager) throws IOException;

    /**
     * is running
     */
    void running();
    
    /**
     * shutdown server
     */
    void shutdown();
}
