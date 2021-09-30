package org.dhp.net.nio;

import lombok.extern.slf4j.Slf4j;
import org.dhp.core.rpc.Message;
import org.dhp.core.rpc.MessageStatus;
import org.dhp.core.rpc.MetaData;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.utils.BufferOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

@Slf4j
public class NioMessage extends Message {

    public NioMessage(Buffer buffer) {
        this.unpack(buffer);
    }
    public NioMessage() {

    }

    String readString(Buffer buf) {
        int len = buf.get();
        byte[] bytes = new byte[len];
        buf.get(bytes);
        return new String(bytes);
    }

    int writeString(ByteArrayOutputStream outputStream, String command) {
        byte[] bytes = command.getBytes();
        int len = bytes.length;
        try {
            outputStream.write((byte) len);
            outputStream.write(bytes);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return len + 1;
    }

    int writeString(BufferOutputStream outputStream, String command) {
        byte[] bytes = command.getBytes();
        int len = bytes.length;
        try {
            outputStream.write((byte) len);
            outputStream.write(bytes);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
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
        BufferOutputStream outputStream = new BufferOutputStream(MessageDecoder.memoryManager);
        int headLen = writeString(outputStream, getCommand());
        MetaData metadata = getMetadata();
        if (metadata != null) {
            int len = 1;
            Map<Integer, String> data = metadata.getData();
            try {
                outputStream.write(data.size());
                for (Integer key : data.keySet()) {
                    ByteBuffer keyBuf = ByteBuffer.allocate(4);
                    keyBuf.putInt(key);
                    outputStream.write(keyBuf.array());
                    len += 4;
                    byte[] bytes = data.get(key).getBytes();
                    outputStream.write(bytes.length);
                    len += 1;
                    outputStream.write(bytes);
                    len += bytes.length;
                }
                outputStream.flush();
                headLen += len;
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                outputStream.write((byte) -1);
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
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
        buffer.put((byte)(this.getStatus().getId()));
        Buffer headBuffer = outputStream.getBuffer();
        headBuffer.flip();
        buffer.put(headBuffer.array(), headBuffer.arrayOffset(), headBuffer.limit());
        if (data != null)
            buffer.put(data);
        buffer.flip();
        return buffer;
    }

}
