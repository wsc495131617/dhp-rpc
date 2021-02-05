package org.dhp.net.netty4;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.dhp.core.rpc.IRpcServer;
import org.dhp.core.rpc.RpcServerMethodManager;
import org.dhp.core.rpc.SessionManager;

/**
 * @author zhangcb
 */
@Slf4j
public class NettyRpcServer implements IRpcServer {

    int port;

    SessionManager sessionManager;

    final ServerBootstrap serverBootstrap = new ServerBootstrap();
    final EventLoopGroup boss = new NioEventLoopGroup(1);
    EventLoopGroup worker;

    int workThread = 4;

    public NettyRpcServer(int port, int workThread) {
        this.port = port;
        this.workThread = workThread;
        this.sessionManager = new NettySessionManager();
    }

    @Override
    public void start(RpcServerMethodManager methodManager) {

        worker = new NioEventLoopGroup(4);

        serverBootstrap.group(boss, worker);
        serverBootstrap.channel(NioServerSocketChannel.class);

        serverBootstrap.childHandler(new ChannelInitializer<Channel>() {
            protected void initChannel(Channel channel) throws Exception {
                ChannelPipeline pipeline = channel.pipeline();
                pipeline.addLast(new RpcMessageEncoder());
                pipeline.addLast(new RpcMessageDecoder());
                pipeline.addLast(new MethodDispatchHandler(methodManager, sessionManager));
            }
        });

        serverBootstrap.option(ChannelOption.SO_BACKLOG, 4096);         //连接缓冲池的大小
        serverBootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        serverBootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);//维持链接的活跃，清除死链接
        serverBootstrap.childOption(ChannelOption.TCP_NODELAY, true);//关闭延迟发送

        Thread awaitThread = new Thread("dhp-netty-waiting-" + this.hashCode()) {
            public void run() {
                try {
                    serverBootstrap.bind("0.0.0.0", port).sync().channel().closeFuture().sync();
                    //服务关闭
                    log.warn("netty is closed!");
                } catch (Exception e) {
                    log.warn("start failed" + e.getMessage(), e);
                }
            }
        };
        awaitThread.setContextClassLoader(this.getClass().getClassLoader());
        awaitThread.setDaemon(false);
        awaitThread.start();
    }

    @Override
    public void running() {

    }

    @Override
    public void shutdown() {
        sessionManager.forceClose();
    }
}