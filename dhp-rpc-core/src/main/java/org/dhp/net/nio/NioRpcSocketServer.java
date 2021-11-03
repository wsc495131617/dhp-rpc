package org.dhp.net.nio;

import lombok.extern.slf4j.Slf4j;
import org.dhp.core.rpc.IRpcServer;
import org.dhp.core.rpc.RpcServerMethodManager;
import org.glassfish.grizzly.jmxbase.GrizzlyJmxManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
public class NioRpcSocketServer implements IRpcServer, Runnable {

    int port;
    int workThread;
    //boss Selector 主要负责accept
    Selector selector;

    //IO 线程 负责read
    NioSelectorThread[] selectorThreads;

    //acceptCount
    int acceptCount = 0;

    ServerSocketChannel serverSocketChannel;
    NioSessionManager sessionManager;
    RpcServerMethodManager methodManager;

    LinkedBlockingQueue<SocketChannel> acceptQueue = new LinkedBlockingQueue<>();

    public NioRpcSocketServer(int port, int workThread) {
        this.port = port;
        if (workThread <= 0) {
            workThread = 4;
        }
        this.workThread = workThread;
        this.sessionManager = new NioSessionManager();
    }

    @Override
    public void start(RpcServerMethodManager methodManager) throws IOException {
        this.methodManager = methodManager;
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.bind(new InetSocketAddress(port));
        selector = Selector.open();
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        //开启IO线程
        selectorThreads = new NioSelectorThread[workThread];
        for (int i = 0; i < workThread; i++) {
            NioSelectorThread nioSelectorThread = new NioSelectorThread(acceptQueue);
            nioSelectorThread.sessionManager = sessionManager;
            nioSelectorThread.methodManager = methodManager;
            nioSelectorThread.setName("NioSelector-" + i);
            nioSelectorThread.setDaemon(true);
            nioSelectorThread.start();
            selectorThreads[i] = nioSelectorThread;
        }

        //开启主循环
        Thread mainThread = new Thread(this);
        mainThread.setName("NioMainLoop");
        mainThread.start();

        //增加监控
        GrizzlyJmxManager jmxManager = GrizzlyJmxManager.instance();
        Object jmxMemoryManagerObject = MessageDecoder.memoryManager.getMonitoringConfig().createManagementObject();
        jmxManager.registerAtRoot(jmxMemoryManagerObject, "nio_memory");
    }

    @Override
    public void run() {
        while (true) {
            try {
                selector.select();
                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    //新进入连接
                    if (key.isAcceptable()) {
                        SocketChannel client = serverSocketChannel.accept();
                        addClient(client);
                        it.remove();
                    }
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    protected void addClient(SocketChannel client) {
        try {
            client.configureBlocking(false);
            acceptQueue.add(client);
            selectorThreads[acceptCount++ % workThread].selector.wakeup();
        } catch (IOException e) {
        }
    }

    @Override
    public void running() {

    }

    @Override
    public void shutdown() {

    }

}
