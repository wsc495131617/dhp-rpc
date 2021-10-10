package org.dhp.net.grizzly;

import lombok.extern.slf4j.Slf4j;
import org.dhp.core.rpc.Message;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.nio.transport.TCPNIOConnection;

import java.io.IOException;

@Slf4j
public class GrizzlyRpcMessageFilter extends BaseFilter {

    @Override
    public NextAction handleRead(FilterChainContext ctx) throws IOException {
        Buffer buffer = ctx.getMessage();
        //当前消息长度
        int sourceBufferLength = buffer.remaining();
        //消息小于4，不符合标准
        if (buffer.remaining() < Message.HEAD_LEN) {
            return ctx.getStopAction(buffer);
        }
        //消息长度
        int len = buffer.getInt(0);
        if (len > Message.MAX_PACKET_LEN || len < 0) {
            byte[] bytes = new byte[buffer.limit()];
            buffer.get(bytes);
            log.error("Buffer String：{}", new String(bytes));
            log.error("PeerAddress:{}", ((TCPNIOConnection) ctx.getConnection()).getPeerAddress());
            log.error("Out of max packet len {}, cur {}，{} close {}!", Message.MAX_PACKET_LEN, len, buffer, ctx.getConnection());
            ctx.getConnection().close();
            return ctx.getStopAction();
        }
        //包不足
        if (buffer.remaining() < len) {
            return ctx.getStopAction(buffer);
        } else {
            Buffer remain = sourceBufferLength > len ? buffer.split(len) : null;
            try {
                GrizzlyMessage msg = new GrizzlyMessage(buffer);
                ctx.setMessage(msg);
                buffer.tryDispose();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                if (remain != null) {
                    return ctx.getInvokeAction(remain);
                } else {
                    return ctx.getStopAction();
                }
            }
            if (remain != null) {
                return ctx.getInvokeAction(remain);
            } else {
                return ctx.getInvokeAction();
            }
        }
    }

    @Override
    public NextAction handleWrite(FilterChainContext ctx) throws IOException {
        GrizzlyMessage message = ctx.getMessage();
        Buffer buffer = message.pack();
        ctx.write(buffer);
        buffer.tryDispose();
        return ctx.getStopAction();
    }
}
