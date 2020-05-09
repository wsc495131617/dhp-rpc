package org.dhp.core.netty4;

import org.dhp.core.rpc.RpcChannel;
import org.dhp.core.rpc.Stream;

import java.util.concurrent.TimeoutException;

public class NettyRpcChannel extends RpcChannel {

    @Override
    public void start() {

    }

    public boolean connect() throws TimeoutException {
        return false;
    }

    @Override
    public Integer write(String name, byte[] argBody, Stream<byte[]> stream) {
        return null;
    }

}
