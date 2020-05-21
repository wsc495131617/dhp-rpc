package org.dhp.net.grizzly;

import org.dhp.core.rpc.IRpcServer;
import org.dhp.core.rpc.RpcServerMethodManager;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.strategies.SameThreadIOStrategy;

import java.io.IOException;

public class GrizzlyRpcServer implements IRpcServer {
    int port;
    GrizzlySessionManager sessionManager;
    public GrizzlyRpcServer(int port) {
        this.port = port;
        this.sessionManager = new GrizzlySessionManager();
    }

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

        TCPNIOTransport transport = builder.build();
        transport.bind(port);
        transport.start();
    }
}
