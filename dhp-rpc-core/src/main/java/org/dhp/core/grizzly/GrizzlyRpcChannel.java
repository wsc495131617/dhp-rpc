package org.dhp.core.grizzly;

import lombok.extern.slf4j.Slf4j;
import org.dhp.core.rpc.RpcChannel;
import org.dhp.core.rpc.Stream;
import org.dhp.core.spring.FrameworkException;
import org.glassfish.grizzly.CompletionHandler;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.nio.transport.TCPNIOConnection;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class GrizzlyRpcChannel extends RpcChannel {

    static TCPNIOTransport transport;

    StreamFilter streamFilter;

    TCPNIOConnection connection;

    AtomicInteger _ID = new AtomicInteger();

    @Override
    public void start() {
        streamFilter = new StreamFilter();
        if(transport == null){
            TCPNIOTransportBuilder builder = TCPNIOTransportBuilder.newInstance();
            FilterChainBuilder fbuilder = FilterChainBuilder.stateless();
            fbuilder.add(new TransportFilter());
            fbuilder.add(new GrizzlyRpcMessageFilter());
            fbuilder.add(streamFilter);

            builder.setProcessor(fbuilder.build());

            this.transport = builder.build();

            try {
                this.transport.start();
            } catch (IOException e) {
                throw new FrameworkException("Grizzly Rpc Channel Start Failed");
            }
        }
        try {
            this.connect();
        } catch (TimeoutException e) {
            throw new FrameworkException("Grizzly Rpc Channel Connect Timeout");
        }
    }

    public boolean connect() throws TimeoutException {
        if(connection != null && connection.isOpen() && connection.canWrite()){
            return true;
        }
        try {
            connection = (TCPNIOConnection) this.transport.connect(this.getHost(), this.getPort()).get(this.getTimeout(), TimeUnit.MILLISECONDS);
            return true;
        } catch (InterruptedException e) {
            log.warn(e.getMessage(), e);
        } catch (ExecutionException e) {
            log.warn(e.getMessage(), e);
        }
        return false;
    }

    public Integer write(String name, byte[] argBody, Stream<byte[]> messageStream) {
        GrizzlyMessage message = new GrizzlyMessage();
        message.setId(_ID.incrementAndGet());
        message.setCommand(name);
        message.setData(argBody);
        CompletionHandler completionHandler = new CompletionHandler<GrizzlyMessage>() {
            public void cancelled() {
                messageStream.onCanceled();
            }
            public void failed(Throwable throwable) {
                messageStream.onFailed(throwable);
            }
            public void completed(GrizzlyMessage message) {
                messageStream.onNext(message.getData());
                messageStream.onCompleted();
            }
            public void updated(GrizzlyMessage message) {
                messageStream.onNext(message.getData());
            }
        };
        streamFilter.setCompleteHandler(message.getId(), completionHandler);
        this.connection.write(message);
        return message.getId();
    }
}
