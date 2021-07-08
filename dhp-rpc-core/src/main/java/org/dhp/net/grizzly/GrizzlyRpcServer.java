package org.dhp.net.grizzly;

import lombok.extern.slf4j.Slf4j;
import org.dhp.core.rpc.IRpcServer;
import org.dhp.core.rpc.RpcServerMethodManager;
import org.glassfish.grizzly.GracefulShutdownListener;
import org.glassfish.grizzly.ShutdownContext;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @author zhangcb
 */
@Slf4j
public class GrizzlyRpcServer implements IRpcServer {
    int port;
    int workThread = 4;
    GrizzlySessionManager sessionManager;
    public GrizzlyRpcServer(int port, int workThread) {
        this.port = port;
        this.workThread = workThread;
        this.sessionManager = new GrizzlySessionManager();
    }
    TCPNIOTransport transport;
    
    @Override
    public void start(RpcServerMethodManager methodManager) throws IOException {
    
        TCPNIOTransportBuilder builder = TCPNIOTransportBuilder.newInstance();
        FilterChainBuilder fbuilder = FilterChainBuilder.stateless();
        fbuilder.add(new TransportFilter());
        fbuilder.add(new GrizzlyRpcMessageFilter());
        fbuilder.add(new MethodDispatchFilter(methodManager, sessionManager));

        builder.setProcessor(fbuilder.build());
        builder.setKeepAlive(true);
        builder.setTcpNoDelay(true);
        builder.setLinger(0);
//        builder.setIOStrategy(SameThreadIOStrategy.getInstance());
        builder.setSelectorThreadPoolConfig(ThreadPoolConfig.defaultConfig().setCorePoolSize(workThread));
        
        transport = builder.build();
        transport.bind(port);
        transport.addShutdownListener(new GracefulShutdownListener() {
            @Override
            public void shutdownRequested(ShutdownContext shutdownContext) {
                log.info("grizzly shutdown requested");
                //等待关闭
                sessionManager.forceClose();
                try {
                    Thread.sleep(1000);
                } catch (Exception e){
                }
            }
            @Override
            public void shutdownForced() {
                System.err.println("shutdown forced!");
            }
        });
        transport.start();

        Thread awaitThread = new Thread("dhp-grizzly-" + this.hashCode()) {
            public void run() {
                running();
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
    public void shutdown(){
        transport.shutdown(1, TimeUnit.SECONDS);
    }
}
