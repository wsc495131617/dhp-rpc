package org.dhp.core.netty4;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.dhp.core.rpc.RpcChannel;
import org.dhp.core.rpc.Stream;
import org.glassfish.grizzly.CompletionHandler;

import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class NettyRpcChannel extends RpcChannel {

    final static EventLoopGroup group = new NioEventLoopGroup(16);

    static Bootstrap b;

    AtomicInteger _ID = new AtomicInteger();

    Channel channel;

    StreamHandler streamHandler;

    @Override
    public void start() {
        streamHandler = new StreamHandler();
        if (b == null) {
            b = new Bootstrap();
            b.group(group).channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        public void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new RpcMessageEncoder());
                            pipeline.addLast(new RpcMessageDecoder());
                            pipeline.addLast(streamHandler); //客户端处理类
                        }
                    });
        }
        try {
            connect();
        } catch (TimeoutException e) {
        }
    }

    public boolean connect() throws TimeoutException {
        if(channel == null || !channel.isOpen()){
            ChannelFuture future = null;
            try {
                future = b.connect(this.getHost(), this.getPort()).sync();
            } catch (InterruptedException e) {
            }
            future.addListener(new ChannelFutureListener() {
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    if(channelFuture.isSuccess()){
                        log.info("Connect {},{},{} success", getName(), getHost(), getPort());
                    } else {
                        log.error("Error", channelFuture.cause());
                    }
                }
            });
            this.channel = future.channel();
        }
        return true;
    }

    @Override
    public Integer write(String name, byte[] argBody, Stream<byte[]> messageStream) {
        NettyMessage message = new NettyMessage();
        message.setId(_ID.incrementAndGet());
        message.setCommand(name);
        message.setData(argBody);
        CompletionHandler completionHandler = new CompletionHandler<NettyMessage>() {
            public void cancelled() {
                messageStream.onCanceled();
            }
            public void failed(Throwable throwable) {
                messageStream.onFailed(throwable);
            }
            public void completed(NettyMessage message) {
                messageStream.onNext(message.getData());
                messageStream.onCompleted();
            }
            public void updated(NettyMessage message) {
                messageStream.onNext(message.getData());
            }
        };
        streamHandler.setCompleteHandler(message.getId(), completionHandler);
        this.channel.writeAndFlush(message);
//        log.info("send message: {}", message);
        return message.getId();
    }

}
