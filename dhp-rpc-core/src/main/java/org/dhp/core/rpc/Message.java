package org.dhp.core.rpc;

import lombok.Data;

@Data
public class Message {
    public static final int HEAD_LEN = 8;
    public static final int MAX_PACKET_LEN = 32*1024*1024;

    int length;
    String command;
    MetaData metadata;
    byte[] data;
}
