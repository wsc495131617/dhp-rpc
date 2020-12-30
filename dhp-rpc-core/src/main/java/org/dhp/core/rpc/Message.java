package org.dhp.core.rpc;

import lombok.Data;
import lombok.ToString;

/**
 * @author zhangcb
 */
@Data
@ToString(exclude = {"data", "next"})
public class Message {
    public static final int HEAD_LEN = 9;
    public static final int MAX_PACKET_LEN = 32 * 1024 * 1024;

    int length;
    Integer id;
    String command;
    MessageStatus status = MessageStatus.Sending;
    MetaData metadata;
    byte[] data;

    //下一个消息，用于连续发送消息时候的处理
    Message next;
}
