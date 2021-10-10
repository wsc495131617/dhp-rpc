package org.dhp.net;

import lombok.extern.slf4j.Slf4j;
import org.dhp.core.rpc.Message;
import org.dhp.core.rpc.MessageStatus;
import org.dhp.core.rpc.MetaData;
import org.dhp.net.nio.MessageDecoder;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.memory.CompositeBuffer;

import java.util.Map;

@Slf4j
public class BufferMessage extends Message {

    public BufferMessage(Buffer buffer) {
        this.unpack(buffer);
    }

    public BufferMessage() {

    }

    String readString(Buffer buf) {
        int len = buf.get();
        byte[] bytes = new byte[len];
        buf.get(bytes);
        return new String(bytes);
    }

    int writeString(CompositeBuffer headBuffer, String command) {
        byte[] bytes = command.getBytes();
        int len = bytes.length;
        Buffer buffer = MessageDecoder.memoryManager.allocate(len + 1);
        buffer.put((byte) len);
        buffer.put(bytes);
        buffer.trim();
        headBuffer.append(buffer);
        return len + 1;
    }

    protected void unpack(Buffer buffer) {
        buffer.position(0);
        int packLen = buffer.getInt();
        this.setLength(packLen);
        this.setId(buffer.getInt());
        this.setStatus(MessageStatus.values()[buffer.get()]);
        this.setCommand(readString(buffer));
        //metadata
        int headLen = buffer.get();
        if (headLen > 0) {
            MetaData metaData = new MetaData();
            for (int i = 0; i < headLen; i++) {
                int metadataId = buffer.getInt();
                String metdataValue = readString(buffer);
                metaData.add(metadataId, metdataValue);
            }
            this.setMetadata(metaData);
        }
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        this.setData(bytes);
    }

    public Buffer pack() {
        CompositeBuffer headBuffer = CompositeBuffer.newBuffer(MessageDecoder.memoryManager);
        int headLen = writeString(headBuffer, getCommand());
        MetaData metadata = getMetadata();
        if (metadata != null) {
            int len = 1;
            Map<Integer, String> data = metadata.getData();
            Buffer buffer = MessageDecoder.memoryManager.allocate(1);
            buffer.put((byte) data.size());
            buffer.trim();
            headBuffer.append(buffer);
            for (Integer key : data.keySet()) {
                byte[] bytes = data.get(key).getBytes();
                buffer = MessageDecoder.memoryManager.allocate(5 + bytes.length);
                buffer.putInt(key);
                buffer.put((byte) bytes.length);
                buffer.put(bytes);
                buffer.trim();
                headBuffer.append(buffer);
                len += 5 + bytes.length;
            }
            headLen += len;
        } else {
            Buffer buffer = MessageDecoder.memoryManager.allocate(1);
            buffer.put((byte) -1);
            buffer.trim();
            headBuffer.append(buffer);
            headLen++;
        }

        int bodyLen = 0;
        byte[] data = this.getData();
        if (data != null) {
            bodyLen = this.getData().length;
        }
        int length = headLen + bodyLen + HEAD_LEN;
        Buffer buffer = MessageDecoder.memoryManager.allocate(length);
        buffer.putInt(length);
        buffer.putInt(this.getId());
        buffer.put((byte) (this.getStatus().getId()));
        buffer.put(headBuffer);
        headBuffer.tryDispose();
        if (data != null)
            buffer.put(data);
        buffer.flip();
        return buffer;
    }

}
