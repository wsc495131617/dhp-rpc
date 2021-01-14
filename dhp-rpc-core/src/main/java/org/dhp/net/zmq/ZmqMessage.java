package org.dhp.net.zmq;

import lombok.extern.slf4j.Slf4j;
import org.dhp.core.rpc.Message;
import org.dhp.core.rpc.MessageStatus;
import org.dhp.core.rpc.MetaData;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

@Slf4j
public class ZmqMessage extends Message {

    ByteBuffer byteBuffer;

    public ZmqMessage(ByteBuffer buffer) {
        this.byteBuffer = buffer;
        this.unpack();
    }

    public ZmqMessage() {

    }

    String readString(ByteBuffer buf) {
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

    protected void unpack() {
        if (log.isDebugEnabled()) {
            log.debug("unpack: {}", byteBuffer);
        }
        byteBuffer.position(0);
        int packLen = byteBuffer.getInt();
        this.setLength(packLen);
        this.setId(byteBuffer.getInt());
        this.setStatus(MessageStatus.values()[byteBuffer.get()]);
        this.setCommand(readString(byteBuffer));
        //metadata
        int headLen = byteBuffer.get();
        if (headLen > 0) {
            MetaData metaData = new MetaData();
            for (int i = 0; i < headLen; i++) {
                int metadataId = byteBuffer.getInt();
                String metdataValue = readString(byteBuffer);
                metaData.add(metadataId, metdataValue);
            }
            this.setMetadata(metaData);
        }
        byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytes);
        this.setData(bytes);
    }

    public ByteBuffer pack() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
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
        ByteBuffer buffer = ByteBuffer.allocate(length);
        buffer.putInt(length);
        buffer.putInt(this.getId());
        buffer.put((byte) (this.getStatus().getId()));
        buffer.put(outputStream.toByteArray());
        if (data != null)
            buffer.put(data);
        if (log.isDebugEnabled()) {
            log.info("pack: {}", buffer);
        }
        buffer.flip();
        return buffer;
    }
}
