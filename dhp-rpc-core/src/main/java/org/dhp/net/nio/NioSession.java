package org.dhp.net.nio;

import org.dhp.core.rpc.Message;
import org.dhp.core.rpc.Session;

import java.nio.channels.SocketChannel;

public class NioSession extends Session {
    SocketChannel channel;
    public NioSession(SocketChannel channel) {
        this.channel = channel;
    }

    @Override
    public void write(Message message) {

    }
}
