package org.dhp.net.nio;

import org.dhp.common.rpc.Stream;
import org.dhp.core.rpc.Message;
import org.dhp.core.rpc.RpcChannel;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.TimeoutException;

public class NioRpcChannel extends RpcChannel {
    @Override
    public void start() {
        try {
            SocketChannel socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            Selector selector = Selector.open();
            socketChannel.register(selector, SelectionKey.OP_CONNECT);
        } catch (IOException e) {

        }

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
