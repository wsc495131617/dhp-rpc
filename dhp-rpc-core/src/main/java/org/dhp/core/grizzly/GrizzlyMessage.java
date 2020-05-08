package org.dhp.core.grizzly;

import org.dhp.core.rpc.Message;
import org.dhp.core.rpc.MetaData;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.memory.HeapBuffer;

public class GrizzlyMessage extends Message {

    public GrizzlyMessage() {

    }

    public GrizzlyMessage(Buffer buffer) {
        this.unpack(buffer);
    }

    String readString(Buffer buffer) {
        int commandLen = buffer.get();
        byte[] bytes = new byte[commandLen];
        buffer.get(bytes);
        return new String(bytes);
    }

    int writeString(GrizzlyOutputStream outputStream, String value) {
        byte[] bytes = value.getBytes();
        outputStream.writeByte(bytes.length);
        outputStream.writeBuffer(HeapBuffer.wrap(bytes));
        return bytes.length+1;
    }

    public void unpack(Buffer buffer) {
        int startPos = buffer.position();
        int packLen = buffer.getInt();
        this.setLength(packLen);
        //跳过预留头部的长度
        buffer.position(buffer.position()+HEAD_LEN-4);
        //找到对应的commandName
        this.setCommand(readString(buffer));
        //head len
        int headLen = buffer.getShort();
        if(headLen>0){
            MetaData metaData = new MetaData();
            for(int i=0;i<headLen;i++){
                int metadataId = buffer.getInt();
                String metdataValue = readString(buffer);
                metaData.add(metadataId, metdataValue);
            }
            this.setMetadata(metaData);
        }
        int headPos = buffer.position();
        byte[] bytes = new byte[packLen - headPos + startPos];
        buffer.get(bytes);
        this.setData(bytes);
    }

    public Buffer pack() {
        GrizzlyOutputStream outputStream = new GrizzlyOutputStream();
        int headLen = writeString(outputStream, getCommand());
        MetaData metadata = getMetadata();
        if(metadata != null) {
            headLen += metadata.write(outputStream);
        }
        int bodyLen = this.getData().length;
        int length = headLen+bodyLen+HEAD_LEN;
        Buffer buffer = GrizzlyGlobal.memoryManager.allocate(length);
        buffer.putInt(length);
        //暂时预留跳过
        buffer.position(HEAD_LEN);
        buffer.put(outputStream.getBuffer());
        buffer.position(0);
        return buffer;
    }
}
