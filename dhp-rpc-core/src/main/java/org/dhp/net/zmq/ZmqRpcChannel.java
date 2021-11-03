package org.dhp.net.zmq;

import org.dhp.common.rpc.Stream;
import org.dhp.core.rpc.Message;
import org.dhp.core.rpc.RpcChannel;
import org.zeromq.SocketType;
import org.zeromq.ZMQ;

import java.util.concurrent.TimeoutException;

public class ZmqRpcChannel extends RpcChannel {

    ZMQ.Context context;
    ZMQ.Socket socket;

    @Override
    public void start() {
        context = ZMQ.context(1);
        socket = context.socket(SocketType.REP);
    }

    @Override
    public boolean isClose() {
        return false;
    }

    @Override
    public boolean connect() throws TimeoutException {
        return false;
    }

    @Override
    public Integer write(String name, byte[] argBody, Stream<Message> stream) {
        return null;
    }

    @Override
    public void close() {

    }
}
