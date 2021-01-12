package org.dhp.net.netty4;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import org.dhp.core.rpc.Message;
import org.dhp.core.rpc.MessageStatus;
import org.dhp.core.rpc.MetaData;

import java.io.IOException;
import java.util.Map;

/**
 * @author zhangcb
 */
@Slf4j
public class NettyMessage extends Message {
    public NettyMessage(ByteBuf buf) {
        unpack(buf);
    }

    public NettyMessage() {
    }

    String readString(ByteBuf buf) {
        int len = buf.readByte();
        byte[] bytes = new byte[len];
        buf.readBytes(bytes);
        return new String(bytes);
    }

    int writeString(ByteBufOutputStream outputStream, String command) {
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

    protected void unpack(ByteBuf buf) {
//        if (log.isDebugEnabled()) {
//            log.debug("unpack: {}", buf);
//        }
        int packLen = buf.readInt();
        this.setLength(packLen);
        this.setId(buf.readInt());
        this.setStatus(MessageStatus.values()[buf.readByte()]);
        this.setCommand(readString(buf));
        //metadata
        int headLen = buf.readByte();
        if (headLen > 0) {
            MetaData metaData = new MetaData();
            for (int i = 0; i < headLen; i++) {
                int metadataId = buf.readInt();
                String metdataValue = readString(buf);
                metaData.add(metadataId, metdataValue);
            }
            this.setMetadata(metaData);
        }
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        this.setData(bytes);
    }

    public ByteBuf pack() {
        ByteBufOutputStream outputStream = new ByteBufOutputStream(Unpooled.buffer());
        int headLen = writeString(outputStream, getCommand());
        MetaData metadata = getMetadata();
        if (metadata != null) {
            int len = 1;
            Map<Integer, String> data = metadata.getData();
            try {
                outputStream.writeByte(data.size());
                for (Integer key : data.keySet()) {
                    outputStream.writeInt(key);
                    len += 4;
                    byte[] bytes = data.get(key).getBytes();
                    outputStream.writeByte(bytes.length);
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
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeInt(length);
        buffer.writeInt(this.getId());
        buffer.writeByte(this.getStatus().getId());
        buffer.writeBytes(outputStream.buffer());
        if (data != null)
            buffer.writeBytes(data);
//        if (log.isDebugEnabled()) {
//            log.info("pack: {}", buffer);
//        }
        return buffer;
    }
}
