package org.dhp.net.grizzly;

import lombok.extern.slf4j.Slf4j;
import org.dhp.common.rpc.SimpleStream;
import org.dhp.common.rpc.Stream;
import org.dhp.common.utils.ProtostuffUtils;
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
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class GrizzlyRpcChannel extends RpcChannel {

    static TCPNIOTransport transport;

    TCPNIOConnection connection;

    AtomicInteger _ID = new AtomicInteger();
    
    static ClientStreamManager streamManager = new ClientStreamManager();
    
    @Override
    public void start() {
        if (transport == null) {
            TCPNIOTransportBuilder builder = TCPNIOTransportBuilder.newInstance();
            FilterChainBuilder fbuilder = FilterChainBuilder.stateless();
            fbuilder.add(new TransportFilter());
            fbuilder.add(new GrizzlyRpcMessageFilter());
            fbuilder.add(new BaseFilter(){
                @Override
                public NextAction handleClose(FilterChainContext ctx) throws IOException {
                    log.info("connection:{} closed!", ctx.getConnection());
                    return super.handleClose(ctx);
                }
    
                @Override
                public NextAction handleRead(FilterChainContext ctx) throws IOException {
                    GrizzlyMessage message = ctx.getMessage();
                    if(log.isDebugEnabled())
                        log.debug("recv: {}", message);
                    //close
                    if(message.getCommand().equals("close")){
                        Connection connection = ctx.getConnection();
                        readyToCloseConns.add(connection);
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
            builder.setIOStrategy(SameThreadIOStrategy.getInstance());

            this.transport = builder.build();

            try {
                this.transport.start();
            } catch (IOException e) {
                throw new FrameworkException("Grizzly Rpc Channel Start Failed");
            }
        }
        this.connect();
    }
    
    public boolean connect() {
        if (connection != null && connection.isOpen() && connection.canWrite()) {
            return true;
        }
        try {
            log.info("connect to {}:{}", this.getHost(), this.getPort());
            connection = (TCPNIOConnection) this.transport.connect(this.getHost(), this.getPort()).get(this.getTimeout(), TimeUnit.MILLISECONDS);
            byte[] idBytes = ProtostuffUtils.serialize(Long.class, this.getId());
            FutureImpl<Message> future = new FutureImpl<>();
            Stream<Message> stream = new SimpleStream<>();
            future.addStream(stream);
            write("register", idBytes, stream);
            Message resp = future.get();
            if(resp != null && resp.getStatus() == MessageStatus.Completed){
                return true;
            }
            return false;
        } catch (InterruptedException e) {
            log.warn(e.getMessage(), e);
        } catch (ExecutionException | TimeoutException e) {
            throw new RpcException(RpcErrorCode.UNREACHABLE_NODE);
        }
        return false;
    }

    private long activeTime = System.currentTimeMillis();
    
    protected GrizzlyMessage sendMessage(String command, byte[] body){
        synchronized (connection){
            while(readyToCloseConns.contains(connection)){
                try {
                    log.info("waiting for switch connection");
                    connect();
                    log.info("switch connection success, {}", connection);
                } catch (RpcException e){
                    if (e.getCode() == RpcErrorCode.UNREACHABLE_NODE) {
                        continue;
                    }
                } finally {
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
        GrizzlyMessage message = new GrizzlyMessage();
        message.setId(_ID.incrementAndGet());
        message.setCommand(command);
        message.setData(body);
        message.setStatus(MessageStatus.Sending);
        this.connection.write(message);
        return message;
    }

    public void ping() {
        GrizzlyMessage message = sendMessage("ping", (System.currentTimeMillis()+"").getBytes());
        Stream<GrizzlyMessage> stream = new Stream<GrizzlyMessage>() {
            public void onCanceled() {
            }
            public void onNext(GrizzlyMessage value) {
                activeTime = System.currentTimeMillis();
                log.info("pong " + new String(value.getData()));
            }
            public void onFailed(Throwable throwable) {
            }
            public void onCompleted() {
            }
        };
        streamManager.setStream(message, stream);
    }

    public Integer write(String name, byte[] argBody, Stream<Message> messageStream) {
        GrizzlyMessage message = sendMessage(name, argBody);
        streamManager.setStream(message, messageStream);
        return message.getId();
    }
}
