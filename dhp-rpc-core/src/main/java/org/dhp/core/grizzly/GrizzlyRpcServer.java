package org.dhp.core.grizzly;

import org.dhp.core.rpc.IRpcServer;
import org.dhp.core.rpc.RpcServerMethodManager;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;

public class GrizzlyRpcServer implements IRpcServer {
    int port;

    public GrizzlyRpcServer(int port) {
        this.port = port;
    }

    public void start(RpcServerMethodManager methodManager) {
        TCPNIOTransportBuilder builder = TCPNIOTransportBuilder.newInstance();
        FilterChainBuilder fbuilder = FilterChainBuilder.stateless();
        fbuilder.add(new TransportFilter());
        fbuilder.add(new GrizzlyRpcMessageFilter());
        fbuilder.add(new MethodDispatchFilter(methodManager));

    }
}
