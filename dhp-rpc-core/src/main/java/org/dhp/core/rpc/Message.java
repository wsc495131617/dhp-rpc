package org.dhp.core.rpc;

import lombok.Data;
import lombok.ToString;

/**
 * @author zhangcb
 */
@Data
@ToString(exclude = "data")
public class Message {
    public static final int HEAD_LEN = 9;
    public static final int MAX_PACKET_LEN = 32 * 1024 * 1024;

    int length;
    Integer id;
    String command;
    MessageStatus status = MessageStatus.Sending;
    MetaData metadata;
    byte[] data;
}
