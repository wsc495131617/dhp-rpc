package org.dhp.net.netty4;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.dhp.common.rpc.Stream;
import org.dhp.core.rpc.*;

/**
 * @author zhangcb
 */
@Slf4j
public class NettyRpcChannel extends RpcChannel {

    Bootstrap b;

    EventLoopGroup group;

    Channel channel;

    ClientStreamManager streamManager;

    @Override
    public void start() {
        streamManager = new ClientStreamManager();
        group = new NioEventLoopGroup();
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
                                rpcChannelPoolGuage.labels(getName(), getHost() + ":" + getPort(), "connect").dec();
                                super.channelInactive(ctx);
                            }

                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                NettyMessage message = (NettyMessage) msg;
//                                if (log.isDebugEnabled()) {
//                                    log.debug("recv: {}", msg);
//                                }
                                //update active time
                                activeTime = System.currentTimeMillis();
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
        connect();
    }

    @Override
    public boolean connect() {
        if (channel == null || !channel.isOpen()) {
            try {
                log.info("connect to {}:{}", this.getHost(), this.getPort());
                this.channel = b.connect(this.getHost(), this.getPort()).sync().channel();
                rpcChannelPoolGuage.labels(getName(), this.getHost() + ":" + this.getPort(), "connect").inc();
                this.active = true;
                this.activeTime = System.currentTimeMillis();
            } catch (Exception e) {
                log.warn("connect failed: "+e.getMessage());
                throw new RpcException(RpcErrorCode.UNREACHABLE_NODE);
            }
            return true;
        }
        return true;
    }

    protected NettyMessage createMessage(String command, byte[] body) {
        NettyMessage message = new NettyMessage();
        message.setId(_ID.incrementAndGet());
        message.setCommand(command);
        message.setData(body);
        message.setStatus(MessageStatus.Sending);
        return message;
    }

    public NettyMessage sendMessage(NettyMessage message) {
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
        this.channel.writeAndFlush(message);
        return message;
    }

    @Override
    public boolean isClose() {
        return channel == null || !channel.isActive();
    }

    @Override
    public Integer write(String name, byte[] argBody, Stream<Message> messageStream) {
        NettyMessage message = createMessage(name, argBody);
        streamManager.setStream(message, messageStream);
        sendMessage(message);
        return message.getId();
    }

    @Override
    public void close() {
        channel.close();
    }
}
