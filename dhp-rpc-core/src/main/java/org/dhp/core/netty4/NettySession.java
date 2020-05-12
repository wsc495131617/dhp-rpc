package org.dhp.core.netty4;

import io.netty.channel.Channel;
import org.dhp.core.rpc.Message;
import org.dhp.core.rpc.Session;

public class NettySession extends Session {
    
    Channel channel;
    
    public NettySession(Channel channel){
        this.channel = channel;
    }
    
    public void write(Message message) {
        this.channel.writeAndFlush(message);
    }
}
