package org.dhp.core.grizzly;

import org.dhp.core.rpc.MessageStatus;
import org.dhp.core.spring.FrameworkException;
import org.glassfish.grizzly.CompletionHandler;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class StreamFilter extends BaseFilter {

    AtomicInteger _ID = new AtomicInteger(1);

    Map<Integer, CompletionHandler> handlerMap = new ConcurrentHashMap<>();

    public Integer incrementAndGet() {
        return _ID.incrementAndGet();
    }

    public NextAction handleRead(FilterChainContext ctx) throws IOException {
        GrizzlyMessage message = ctx.getMessage();
        if(handlerMap.containsKey(message.getId())){
            CompletionHandler handler = handlerMap.get(message.getId());
            MessageStatus status = message.getStatus();
            switch (status){
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
        return ctx.getStopAction();
    }

    public Throwable dealThrowable(GrizzlyMessage message){
        return null;
    }

    public void setCompleteHandler(Integer id, CompletionHandler completionHandler) {
        handlerMap.put(id, completionHandler);
    }
}
