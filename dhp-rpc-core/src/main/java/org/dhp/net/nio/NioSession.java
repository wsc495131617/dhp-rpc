package org.dhp.net.nio;

import org.dhp.core.rpc.Message;
import org.dhp.core.rpc.RpcErrorCode;
import org.dhp.core.rpc.RpcException;
import org.dhp.core.rpc.Session;
import org.glassfish.grizzly.Buffer;

import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * 缓存Session的写缓存
 */
public class NioSession extends Session {

    MessageDecoder messageDecoder;

    SocketChannel channel;

    public NioSession(SocketChannel channel) {
        this.channel = channel;
    }

    public void setMessageDecoder(MessageDecoder messageDecoder) {
        this.messageDecoder = messageDecoder;
    }

    public MessageDecoder getMessageDecoder() {
        return messageDecoder;
    }

    @Override
    public void write(Message message) {
        Buffer buf = ((NioMessage) message).pack();
        try {
            channel.write(buf.toByteBuffer());
        } catch (IOException e) {
            throw new RpcException(RpcErrorCode.SYSTEM_ERROR);
        } finally {
            buf.dispose();
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        this.messageDecoder.destroy();
    }

    @Override
    public String toString() {
        return "NioSession{" +
                "channel=" + channel +
                '}';
    }
}
