package org.dhp.net.netty4;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RpcMessageEncoder extends MessageToByteEncoder {

    protected void encode(ChannelHandlerContext channelHandlerContext, Object o, ByteBuf byteBuf) throws Exception {
        NettyMessage message = (NettyMessage) o;
        ByteBuf buf = message.pack();
        byteBuf.writeBytes(buf);
        buf.release();
    }
}
