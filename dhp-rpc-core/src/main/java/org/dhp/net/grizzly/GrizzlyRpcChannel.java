package org.dhp.net.grizzly;

import lombok.extern.slf4j.Slf4j;
import org.dhp.common.rpc.Stream;
import org.dhp.core.rpc.*;
import org.dhp.core.spring.FrameworkException;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.filterchain.*;
import org.glassfish.grizzly.nio.transport.TCPNIOConnection;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.strategies.SameThreadIOStrategy;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author zhangcb
 */
@Slf4j
public class GrizzlyRpcChannel extends RpcChannel {

    static TCPNIOTransport transport;
    static Object lock = new Object();

    TCPNIOConnection connection;

    static ClientStreamManager streamManager = new ClientStreamManager();

    @Override
    public void start() {
        if (transport == null) {
            synchronized (lock) {
                if (transport == null) {
                    TCPNIOTransportBuilder builder = TCPNIOTransportBuilder.newInstance();
                    FilterChainBuilder fbuilder = FilterChainBuilder.stateless();
                    fbuilder.add(new TransportFilter());
                    fbuilder.add(new GrizzlyRpcMessageFilter());
                    fbuilder.add(new BaseFilter() {
                        @Override
                        public NextAction handleClose(FilterChainContext ctx) throws IOException {
                            Connection connection = ctx.getConnection();
                            readyToCloseConns.remove(connection);
                            log.info("connection:{} closed!", connection);
                            return super.handleClose(ctx);
                        }

                        @Override
                        public NextAction handleRead(FilterChainContext ctx) throws IOException {
                            GrizzlyMessage message = ctx.getMessage();
//                    log.debug("{}, recv: {},{}", getId(), message, ctx.getConnection());
                            //update active time
                            activeTime = System.currentTimeMillis();
                            //waiting to close message, reject all new request
                            if (message.getCommand().equals("close")) {
                                Connection connection = ctx.getConnection();
                                readyToCloseConns.add(connection);
                                active = false;
                            } else {
                                streamManager.handleMessage(message);
                            }
                            return super.handleRead(ctx);
                        }
                    });

                    builder.setProcessor(fbuilder.build());

                    builder.setTcpNoDelay(true);
                    builder.setKeepAlive(true);
                    builder.setLinger(0);
                    //作为客户端，线程切换的事情交给发送端
                    builder.setIOStrategy(SameThreadIOStrategy.getInstance());

                    transport = builder.build();

                    try {
                        transport.start();
                    } catch (IOException e) {
                        throw new FrameworkException("Grizzly Rpc Channel Start Failed");
                    }
                }
            }
        }
        this.connect();
    }

    @Override
    public boolean connect() {
        if (!isClose()) {
            return true;
        }
        try {
            connection = (TCPNIOConnection) transport.connect(this.getHost(), this.getPort()).get(this.getTimeout(), TimeUnit.MILLISECONDS);
            log.info("connect to {} {}:{}, {}", this.getName(), this.getHost(), this.getPort(), connection);
            rpcChannelPoolGuage.labels(getName(), this.getHost() + ":" + this.getPort(), "connect").inc();
            this.active = true;
            this.activeTime = System.currentTimeMillis();
            return true;
        } catch (InterruptedException e) {
            log.warn(e.getMessage(), e);
        } catch (ExecutionException | TimeoutException e) {
            throw new RpcException(RpcErrorCode.UNREACHABLE_NODE);
        }
        return false;
    }

    @Override
    public boolean isClose() {
        return connection == null || (connection != null && connection.isOpen() && connection.canWrite());
    }

    protected GrizzlyMessage createMessage(String command, byte[] body) {
        GrizzlyMessage message = new GrizzlyMessage();
        message.setId(_ID.incrementAndGet());
        message.setCommand(command);
        message.setData(body);
        message.setStatus(MessageStatus.Sending);
        return message;
    }

    protected GrizzlyMessage sendMessage(GrizzlyMessage message) {
        checkConnection();
        this.connection.write(message);
        return message;
    }

    private void checkConnection() {
        synchronized (connection) {
            long st = System.currentTimeMillis();
            //不超过5s以内进行重试，如果真连不上，就放弃当前channel
            while (readyToCloseConns.contains(connection) && System.currentTimeMillis() - st > 5000) {
                try {
                    log.info("waiting for switch connection");
                    connect();
                    log.info("switch connection success, {}", connection);
                    return;
                } catch (RpcException e) {
                    if (e.getCode() == RpcErrorCode.UNREACHABLE_NODE) {
                        continue;
                    }
                } finally {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
    }

    @Override
    public Integer write(String name, byte[] argBody, Stream<Message> messageStream) {
        GrizzlyMessage message = createMessage(name, argBody);
        streamManager.setStream(message, messageStream);
        sendMessage(message);
        return message.getId();
    }

    @Override
    public void close() {
        if (!isClose()) {
            connection.close();
        }
    }
}
