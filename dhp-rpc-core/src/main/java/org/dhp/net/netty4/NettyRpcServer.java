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

@Slf4j
public class NettyRpcServer implements IRpcServer {

    int port;
    
    SessionManager sessionManager;

    public NettyRpcServer(int port) {
        this.port = port;
        this.sessionManager = new NettySessionManager();
    }

    public void start(RpcServerMethodManager methodManager) {
        final ServerBootstrap serverBootstrap = new ServerBootstrap();

        EventLoopGroup boss = new NioEventLoopGroup(1);
        EventLoopGroup worker = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() * 2);

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

        //7.5.监听关闭
        Thread t = new Thread(() -> {
            try {
                //7.绑定ip和port
                ChannelFuture channelFuture = serverBootstrap.bind("0.0.0.0", port).sync();//Future模式的channel对象
                channelFuture.channel().closeFuture().sync();  //等待服务关闭，关闭后应该释放资源
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                //8.优雅的关闭资源
                boss.shutdownGracefully();
                worker.shutdownGracefully();
            }
        });
        t.setDaemon(false);
        t.start();
    }
}
