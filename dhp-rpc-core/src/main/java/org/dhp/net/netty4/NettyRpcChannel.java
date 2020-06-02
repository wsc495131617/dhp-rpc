package org.dhp.net.netty4;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.dhp.common.rpc.SimpleStream;
import org.dhp.common.rpc.Stream;
import org.dhp.common.utils.ProtostuffUtils;
import org.dhp.core.rpc.*;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zhangcb
 */
@Slf4j
public class NettyRpcChannel extends RpcChannel {
    
    final static EventLoopGroup group = new NioEventLoopGroup();
    
    static Bootstrap b;
    
    AtomicInteger _ID = new AtomicInteger();
    
    Channel channel;
    
    ClientStreamManager streamManager;
    
    @Override
    public void start() {
        streamManager = new ClientStreamManager();
        if (b == null) {
            b = new Bootstrap();
            b.option(ChannelOption.SO_KEEPALIVE, true);
            b.option(ChannelOption.TCP_NODELAY, true);
            b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);
            
            b.group(group).channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        public void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new RpcMessageEncoder());
                            pipeline.addLast(new RpcMessageDecoder());
                            pipeline.addLast(new ChannelInboundHandlerAdapter() {
                                
                                @Override
                                public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                                    log.info("channel is closed: {}", ctx.channel());
                                    //cancel all req
                                    super.channelInactive(ctx);
                                }
                                
                                @Override
                                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                    NettyMessage message = (NettyMessage) msg;
                                    if(log.isDebugEnabled()) {
                                        log.debug("recv: {}", msg);
                                    }
                                    //close
                                    if (message.getCommand().equals("close")) {
                                        Channel channel = ctx.channel();
                                        readyToCloseConns.add(channel);
                                    } else {
                                        streamManager.handleMessage(message);
                                    }
                                }
                            }); //客户端处理类
                        }
                    });
        }
        connect();
    }
    @Override
    public boolean connect() {
        if (channel == null || !channel.isOpen()) {
            ChannelFuture future = null;
            try {
                log.info("connect to {}:{}", this.getHost(), this.getPort());
                future = b.connect(this.getHost(), this.getPort()).sync();
            } catch (Exception e) {
                throw new RpcException(RpcErrorCode.UNREACHABLE_NODE);
            }
            this.channel = future.channel();
            byte[] idBytes = ProtostuffUtils.serialize(Long.class, this.getId());
            FutureImpl<Message> mfuture = new FutureImpl<>();
            Stream<Message> stream = new SimpleStream<Message>(){
                @Override
                public void onNext(Message value) {
                    mfuture.result(value);
                }
            };
            mfuture.addStream(stream);
            write("register", idBytes, stream);
            Message resp = null;
            try {
                resp = mfuture.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            if(resp != null && resp.getStatus() == MessageStatus.Completed){
                return true;
            }
            return false;
        }
        return true;
    }
    
    private long activeTime = System.currentTimeMillis();
    
    public NettyMessage sendMessage(String command, byte[] body) {
        synchronized (channel) {
            while (readyToCloseConns.contains(channel)) {
                try {
                    log.warn("waiting for switch channel");
                    connect();
                    log.warn("switch channel success!");
                } catch (RpcException e) {
                    if (e.getCode() == RpcErrorCode.UNREACHABLE_NODE) {
                        continue;
                    }
                } finally {
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
        
        NettyMessage message = new NettyMessage();
        message.setId(_ID.incrementAndGet());
        message.setCommand(command);
        message.setData(body);
        message.setStatus(MessageStatus.Sending);
        this.channel.writeAndFlush(message);
        return message;
    }
    
    @Override
    public void ping() {
        Stream<NettyMessage> stream = new Stream<NettyMessage>() {
            @Override
            public void onCanceled() {
            }
            @Override
            public void onNext(NettyMessage value) {
                activeTime = System.currentTimeMillis();
                log.info("pong " + new String(value.getData()));
            }
            @Override
            public void onFailed(Throwable throwable) {
            }
            @Override
            public void onCompleted() {
            }
        };
        NettyMessage message = sendMessage("ping", (System.currentTimeMillis() + "").getBytes());
        streamManager.setStream(message, stream);
    }
    
    @Override
    public Integer write(String name, byte[] argBody, Stream<Message> messageStream) {
        NettyMessage message = sendMessage(name, argBody);
        streamManager.setStream(message, messageStream);
        return message.getId();
    }
    
}
