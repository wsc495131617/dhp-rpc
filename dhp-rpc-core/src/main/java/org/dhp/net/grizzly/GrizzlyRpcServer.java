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
import org.glassfish.grizzly.strategies.SameThreadIOStrategy;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
public class GrizzlyRpcServer implements IRpcServer {
    int port;
    GrizzlySessionManager sessionManager;
    public GrizzlyRpcServer(int port) {
        this.port = port;
        this.sessionManager = new GrizzlySessionManager();
    }
    TCPNIOTransport transport;
    
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
        builder.setIOStrategy(SameThreadIOStrategy.getInstance());
        
        transport = builder.build();
        transport.bind(port);
        transport.addShutdownListener(new GracefulShutdownListener() {
            public void shutdownRequested(ShutdownContext shutdownContext) {
                log.info("grizzly shutdown requested");
                //等待关闭
                sessionManager.forceClose();
                try {
                    Thread.sleep(1000);
                } catch (Exception e){
                
                }
            }
            public void shutdownForced() {
                log.info("grizzly shutdown forced");
            }
        });
        transport.start();
    }
    
    public void shutdown(){
        transport.shutdown(1, TimeUnit.SECONDS);
    }
}
