package org.dhp.net.nio;

import org.dhp.core.rpc.MessageStatus;
import org.dhp.core.rpc.MetaData;
import org.dhp.net.BufferMessage;

public class NioMessageBuilder {
    String command;
    MessageStatus status;
    MetaData metadata;
    byte[] data;

    public NioMessageBuilder setCommand(String command) {
        this.command = command;
        return this;
    }

    public NioMessageBuilder setStatus(MessageStatus status) {
        this.status = status;
        return this;
    }

    public NioMessageBuilder setMetadata(MetaData metadata) {
        this.metadata = metadata;
        return this;
    }

    public NioMessageBuilder setData(byte[] data) {
        this.data = data;
        return this;
    }

    public BufferMessage build() {
        BufferMessage message = new BufferMessage();
        message.setStatus(status);
        message.setMetadata(metadata);
        message.setCommand(command);
        message.setData(data);
        return message;
    }
}
