package org.dhp.core.rpc;

import lombok.Data;

/**
 * Rpc 通道，用于发送信息
 */
@Data
public class RpcChannel {
    String name;
    int port;
    String host;
    long timeout;
}
