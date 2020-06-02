package org.dhp.core.rpc;

/**
 * 通道类型
 * 可以通过不同的网络框架进行通信
 * @author zhangcb
 */
public enum ChannelType {
    Grizzly, Netty, NIO, AIO, ZMQ
}
