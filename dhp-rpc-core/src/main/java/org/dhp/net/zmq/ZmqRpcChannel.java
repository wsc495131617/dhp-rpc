package org.dhp.net.zmq;

import lombok.extern.slf4j.Slf4j;
import org.dhp.common.rpc.Stream;
import org.dhp.core.rpc.ClientStreamManager;
import org.dhp.core.rpc.Message;
import org.dhp.core.rpc.MessageStatus;
import org.dhp.core.rpc.RpcChannel;
import org.dhp.net.nio.MessageDecoder;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * 通过ZMQ的Req模式，去请求Rpc请求
 */
@Slf4j
public class ZmqRpcChannel extends RpcChannel {
    static ZContext zContext;

    static synchronized ZContext getContext() {
        if(zContext == null){
            zContext = new ZContext();
        }
        return zContext;
    }

    ZMQ.Socket socket;
    MessageDecoder messageDecoder;
    ClientStreamManager streamManager;

    @Override
    public void start() {
        try {
            messageDecoder = new MessageDecoder(256);
            streamManager = new ClientStreamManager();
            connect();
        } catch (TimeoutException e) {
        }
    }

    @Override
    public boolean isClose() {
        return false;
    }

    @Override
    public boolean connect() throws TimeoutException {
        if(socket == null || (socket.errno() != 0 && socket.errno() != 35)) {
            socket = getContext().createSocket(SocketType.REQ);
            try {
                return socket.connect("tcp://" + this.getHost() + ":" + this.getPort());
            } catch (ZMQException e) {
                log.error(e.getMessage(), e);
                return false;
            }
        }
        return true;
    }

    @Override
    public Integer write(String name, byte[] argBody, Stream<Message> messageStream) {
        ZmqMessage message = null;
        try {
            message = sendMessage(name, argBody);
            streamManager.setStream(message, messageStream);
            return message.getId();
        } catch (IOException | TimeoutException e) {
            return 0;
        }
    }

    public ZmqMessage sendMessage(String command, byte[] body) throws IOException, TimeoutException {
        ZmqMessage message = new ZmqMessage();
        message.setId(_ID.incrementAndGet());
        message.setCommand(command);
        message.setData(body);
        message.setStatus(MessageStatus.Sending);
        this.socket.send(message.pack().array());
        return message;
    }

    @Override
    public void close() {

    }
}
