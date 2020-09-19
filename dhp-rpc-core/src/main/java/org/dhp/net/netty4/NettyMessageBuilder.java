package org.dhp.net.netty4;

import org.dhp.core.rpc.MessageStatus;
import org.dhp.core.rpc.MetaData;

public class NettyMessageBuilder {
    String command;
    MessageStatus status;
    MetaData metadata;
    byte[] data;

    public NettyMessageBuilder setCommand(String command) {
        this.command = command;
        return this;
    }

    public NettyMessageBuilder setStatus(MessageStatus status) {
        this.status = status;
        return this;
    }

    public NettyMessageBuilder setMetadata(MetaData metadata) {
        this.metadata = metadata;
        return this;
    }

    public NettyMessageBuilder setData(byte[] data) {
        this.data = data;
        return this;
    }

    public NettyMessage build() {
        NettyMessage message = new NettyMessage();
        message.setStatus(status);
        message.setMetadata(metadata);
        message.setCommand(command);
        message.setData(data);
        return message;
    }


}
