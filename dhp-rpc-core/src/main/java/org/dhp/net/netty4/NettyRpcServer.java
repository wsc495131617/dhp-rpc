package org.dhp.net.netty4;

import com.google.common.collect.Lists;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBufAllocatorMetric;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.internal.PlatformDependent;
import io.prometheus.client.GaugeMetricFamily;
import io.prometheus.client.hotspot.DefaultExports;
import lombok.extern.slf4j.Slf4j;
import org.dhp.core.rpc.IRpcServer;
import org.dhp.core.rpc.RpcServerMethodManager;
import org.dhp.core.rpc.SessionManager;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

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
        DefaultExports.initialize();
        (new NettyDirectMemoryExporter()).register();
    }

    @Override
    public void shutdown() {
        sessionManager.forceClose();
    }

    private static class NettyDirectMemoryExporter extends io.prometheus.client.Collector {

        void addMemoryPoolMetrics(List<MetricFamilySamples> sampleFamilies) {
            ByteBufAllocatorMetric unpooledByteBufAllocator = UnpooledByteBufAllocator.DEFAULT.metric();
            ByteBufAllocatorMetric pooledByteBufAllocator = PooledByteBufAllocator.DEFAULT.metric();

            GaugeMetricFamily byteBufGauge = new GaugeMetricFamily(
                    "netty_ByteBuf_Allocator",
                    "ByteBufAllocator",
                    Lists.newArrayList("allocType", "Allocator", "memory"));
            sampleFamilies.add(byteBufGauge);

            byteBufGauge.addMetric(Lists.newArrayList("unpooled", "default", "direct"), unpooledByteBufAllocator.usedDirectMemory());
            byteBufGauge.addMetric(Lists.newArrayList("unpooled", "default", "heap"), unpooledByteBufAllocator.usedHeapMemory());
            byteBufGauge.addMetric(Lists.newArrayList("pooled", "default", "direct"), pooledByteBufAllocator.usedDirectMemory());
            byteBufGauge.addMetric(Lists.newArrayList("pooled", "default", "heap"), pooledByteBufAllocator.usedHeapMemory());

            try {
                Field[] fields = PlatformDependent.class.getDeclaredFields();
                for (Field field : fields) {
                    if (field.getName().equals("DIRECT_MEMORY_COUNTER")) {
                        field.setAccessible(true);
                        AtomicLong counter = (AtomicLong) field.get(null);
                        byteBufGauge.addMetric(Lists.newArrayList("PlatformDependent", "default", "DIRECT_MEMORY_COUNTER"), counter.get());
                    }
                    // -Dio.netty.maxDirectMemory: 3817865216 bytes
                    //
                    if (field.getName().equals("MAX_DIRECT_MEMORY")) {
                        field.setAccessible(true);
                        byteBufGauge.addMetric(Lists.newArrayList("PlatformDependent", "default", "MAX_DIRECT_MEMORY"), field.getLong(null));
                    }
                }
            } catch (Exception e) {
                // ignore
            }

        }

        @Override
        public List<MetricFamilySamples> collect() {
            List<MetricFamilySamples> mfs = new ArrayList<MetricFamilySamples>();
            addMemoryPoolMetrics(mfs);
            return mfs;
        }
    }
}
