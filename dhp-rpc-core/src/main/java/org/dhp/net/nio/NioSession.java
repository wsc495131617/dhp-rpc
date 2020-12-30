package org.dhp.net.nio;

import org.dhp.core.rpc.Message;
import org.dhp.core.rpc.Session;

import java.io.IOException;
import java.nio.channels.SocketChannel;

public class NioSession extends Session {
    SocketChannel channel;

    MessageDecoder messageDecoder;

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
        try {
            //每秒重复发送，那么就缓存，异步统一写入
            if(this.incrementCount()>1){

            }
            channel.write(((NioMessage) message).pack());
        }catch (IOException e){
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        this.messageDecoder.destroy();
    }
}
