package org.dhp.net.netty4;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class RpcMessageDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf in, List<Object> list) throws Exception {
        if (in.readableBytes() < 4) {
            return;
        }
        in.markReaderIndex();
        int dataLength = in.readInt();
        //最大包，用于判断telnet
        if (dataLength > 32 * 1024 * 1024) {
            byte[] bytes = new byte[in.readableBytes()];
            in.readBytes(bytes);
            log.warn("获得文本请求：{}，断开连接", new String(bytes));
            channelHandlerContext.close();
            return;
        }
        if (in.readableBytes() < dataLength - 4) { //读到的消息体长度如果小于我们传送过来的消息长度，则resetReaderIndex. 这个配合markReaderIndex使用的。把readIndex重置到mark的地方
            in.resetReaderIndex();
            return;
        }
        while (in.readableBytes() >= dataLength - 4) {
            in.resetReaderIndex();
            ByteBuf msgBuf = Unpooled.buffer();
            in.readBytes(msgBuf, dataLength);
            NettyMessage msg = new NettyMessage(msgBuf);
            list.add(msg);
            if (in.readableBytes() < 4) {
                return;
            }
            in.markReaderIndex();
            dataLength = in.readInt();
            if (in.readableBytes() < dataLength - 4) {
                in.resetReaderIndex();
                return;
            }
        }
    }

}
