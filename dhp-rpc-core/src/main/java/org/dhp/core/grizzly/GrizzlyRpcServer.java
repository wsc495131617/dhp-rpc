package org.dhp.core.grizzly;

import org.dhp.core.rpc.IRpcServer;
import org.dhp.core.rpc.RpcServerMethodManager;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;

import java.io.IOException;

public class GrizzlyRpcServer implements IRpcServer {
    int port;

    public GrizzlyRpcServer(int port) {
        this.port = port;
    }

    public void start(RpcServerMethodManager methodManager) throws IOException {
        TCPNIOTransportBuilder builder = TCPNIOTransportBuilder.newInstance();
        FilterChainBuilder fbuilder = FilterChainBuilder.stateless();
        fbuilder.add(new TransportFilter());
        fbuilder.add(new GrizzlyRpcMessageFilter());
        fbuilder.add(new MethodDispatchFilter(methodManager));

        builder.setProcessor(fbuilder.build());

        TCPNIOTransport transport = builder.build();
        transport.bind(port);
        transport.start();
    }
}
