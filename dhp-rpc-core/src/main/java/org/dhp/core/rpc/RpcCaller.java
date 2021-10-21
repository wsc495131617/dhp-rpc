package org.dhp.core.rpc;

import lombok.extern.slf4j.Slf4j;
import org.dhp.common.rpc.Stream;
import org.dhp.common.utils.ProtostuffUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
public class RpcCaller {
    @Resource
    RpcChannelPool rpcChannelPool;

    public <T> T call(String nodeName, String commandName, Object arg, Class<T> respCls) {
        RpcChannel channel = rpcChannelPool.getChannel(nodeName);
        return call(channel, commandName, arg, respCls);
    }

    public <T> T call(RpcChannel channel, String commandName, Object arg, final Class<T> respCls) {
        long ts = System.nanoTime();
        FutureImpl<T> finalFuture = new FutureImpl();
        Stream<Message> stream = new Stream<Message>() {
            long ts = System.nanoTime();
            @Override
            public void onCanceled() {
                finalFuture.cancel(false);
                Message.requestLatency.labels("clientInvoked", commandName, MessageStatus.Canceled.name()).observe(System.nanoTime() - this.ts);
            }

            @Override
            public void onNext(Message value) {
                T ret = ProtostuffUtils.deserialize(value.getData(), respCls);
                finalFuture.result(ret);
                Message.requestLatency.labels("clientInvoked", commandName, MessageStatus.Updating.name()).observe(System.nanoTime() - this.ts);
            }

            @Override
            public void onFailed(Throwable throwable) {
                finalFuture.failure(throwable);
                Message.requestLatency.labels("clientInvoked", commandName, MessageStatus.Failed.name()).observe(System.nanoTime() - this.ts);
            }

            @Override
            public void onCompleted() {
                Message.requestLatency.labels("clientInvoked", commandName, MessageStatus.Completed.name()).observe(System.nanoTime() - this.ts);
            }
        };
        byte[] argBody = ProtostuffUtils.serialize(arg);
        channel.write(commandName, argBody, stream);
        try {
            return finalFuture.get(15000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException e) {
            throw new RpcException(RpcErrorCode.UNKNOWN_EXEPTION);
        } catch (TimeoutException e) {
            Message.requestLatency.labels("clientInvoked", commandName, MessageStatus.Timeout.name()).observe(System.nanoTime() - ts);
            throw new RpcException(RpcErrorCode.TIMEOUT);
        }
    }
}
