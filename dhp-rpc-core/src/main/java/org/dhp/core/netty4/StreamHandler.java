package org.dhp.core.netty4;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.dhp.core.rpc.Message;
import org.dhp.core.rpc.MessageStatus;
import org.dhp.core.spring.FrameworkException;
import org.glassfish.grizzly.CompletionHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class StreamHandler extends ChannelInboundHandlerAdapter {

    AtomicInteger _ID = new AtomicInteger(1);

    Map<Integer, CompletionHandler> handlerMap = new ConcurrentHashMap<>();

    public Integer incrementAndGet() {
        return _ID.incrementAndGet();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        NettyMessage message = (NettyMessage) msg;
        if (!handlerMap.containsKey(message.getId())) {
            return;
        }
        CompletionHandler handler = handlerMap.get(message.getId());
        MessageStatus status = message.getStatus();
        switch (status) {
            case Canceled:
                handler.cancelled();
                break;
            case Completed:
                handler.completed(message);
                handlerMap.remove(message.getId());
                break;
            case Updating:
                handler.updated(message);
                break;
            case Failed:
                handler.failed(dealThrowable(message));
                break;
            default:
                throw new FrameworkException("Sending MessageStatus can't response");

        }
    }
    public Throwable dealThrowable(Message message){
        return null;
    }

    public void setCompleteHandler(Integer id, CompletionHandler completionHandler) {
        handlerMap.put(id, completionHandler);
    }
}
