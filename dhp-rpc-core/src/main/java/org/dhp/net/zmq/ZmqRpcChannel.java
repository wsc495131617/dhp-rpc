package org.dhp.net.zmq;

import lombok.extern.slf4j.Slf4j;
import org.dhp.common.rpc.Stream;
import org.dhp.core.rpc.Message;
import org.dhp.core.rpc.MessageStatus;
import org.dhp.core.rpc.RpcChannel;
import org.dhp.net.BufferMessage;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.memory.HeapBuffer;
import org.zeromq.SocketType;
import org.zeromq.ZMQ;

@Slf4j
public class ZmqRpcChannel extends RpcChannel {

    ZMQ.Context context;
    ZMQ.Socket socket;

    @Override
    public void start() {
        context = ZMQ.context(1);
        socket = context.socket(SocketType.REQ);
        connect();
    }

    @Override
    public boolean isClose() {
        return false;
    }

    @Override
    public boolean connect() {
        try {
            boolean ret = socket.connect("tcp://" + this.getHost() + ":" + this.getPort());
            log.info("zmq connect: {}:{}", this.getHost(), this.getPort());
            this.active = true;
            this.activeTime = System.currentTimeMillis();
            return ret;
        } catch (Exception e) {
            return false;
        }
    }

    protected BufferMessage createMessage(String command, byte[] body) {
        BufferMessage message = new BufferMessage();
        message.setId(_ID.incrementAndGet());
        message.setCommand(command);
        message.setData(body);
        message.setStatus(MessageStatus.Sending);
        return message;
    }

    @Override
    public void ping() {
    }

    @Override
    public synchronized Integer write(String name, byte[] argBody, Stream<Message> stream) {
        BufferMessage bufferMessage = createMessage(name, argBody);
        Buffer buffer = bufferMessage.pack();
        byte[] buf = new byte[buffer.limit()];
        System.arraycopy(buffer.array(), buffer.arrayOffset(), buf, 0, buffer.limit());
        socket.send(buf);
        buffer.release();
        byte[] bytes = socket.recv();
        buffer = HeapBuffer.wrap(bytes);
        stream.onNext(new BufferMessage(buffer));
        return 0;
    }

    @Override
    public void close() {

    }
}
