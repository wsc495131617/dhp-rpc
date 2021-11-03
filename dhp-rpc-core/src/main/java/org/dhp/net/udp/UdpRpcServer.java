package org.dhp.net.udp;

import org.dhp.core.rpc.IRpcServer;
import org.dhp.core.rpc.RpcServerMethodManager;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class UdpRpcServer implements IRpcServer {

    DatagramPacket packet;

    DatagramSocket socket;

    int port;

    public UdpRpcServer(int port) {
        this.port = port;
    }

    @Override
    public void start(RpcServerMethodManager methodManager) throws IOException {
        socket = new DatagramSocket(port);
    }

    @Override
    public void running() {

    }

    @Override
    public void shutdown() {

    }
}
