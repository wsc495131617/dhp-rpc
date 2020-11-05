package org.dhp.net.nio;

import lombok.extern.slf4j.Slf4j;
import org.dhp.core.rpc.IRpcServer;
import org.dhp.core.rpc.RpcServerMethodManager;
import org.dhp.core.rpc.Session;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

@Slf4j
public class NioRpcSocketServer implements IRpcServer, Runnable {

    int port;
    int workThread;
    Selector selector;
    ServerSocketChannel serverSocketChannel;
    NioSessionManager sessionManager;

    public NioRpcSocketServer(int port, int workThread) {
        this.port = port;
        this.workThread = workThread;
        this.sessionManager = new NioSessionManager();
    }

    @Override
    public void start(RpcServerMethodManager methodManager) throws IOException {
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.bind(new InetSocketAddress(port));
        selector = Selector.open();
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        log.info("start at {}", port);
        //开启主循环
        Thread mainThread = new Thread(this);
        mainThread.setName("NioMainLoop");
        mainThread.start();
    }

    @Override
    public void run() {
        while (true) {
            try {
                selector.select();
                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    it.remove();
                    //新进入连接
                    if (key.isAcceptable()) {
                        SocketChannel socket = serverSocketChannel.accept();
                        socket.configureBlocking(false);
                        //注册
                        socket.register(selector, SelectionKey.OP_READ);
                    }
                    if (key.isReadable()) {
                        SocketChannel socket = (SocketChannel) key.channel();
                        readChannel(socket);
                    }
                }
            } catch (IOException e) {

            }
        }
    }

    //读取连接数据
    protected void readChannel(SocketChannel socket) {
        NioSession session = (NioSession)sessionManager.getSession(socket);
        //socket 读取 bytes到session里面
        //TODO 
    }

    @Override
    public void running() {

    }

    @Override
    public void shutdown() {

    }
}
