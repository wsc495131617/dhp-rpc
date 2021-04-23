package org.dhp.core.rpc;

import io.prometheus.client.Histogram;
import lombok.Data;
import lombok.ToString;

/**
 * @author zhangcb
 */
@Data
@ToString(exclude = {"data", "next", "ts"})
public class Message {
    public static final int HEAD_LEN = 9;
    public static final int MAX_PACKET_LEN = 32 * 1024 * 1024;

    /**
     * 记录消息从发起，到收到的总时长
     */
    public static final Histogram requestLatency = Histogram.build()
            .name("rpc_requests_latency_nanoseconds")
            .help("rpc latency in nanoseconds.")
            .labelNames("type", "command", "status")
            .buckets(new double[]{1, 10, 100, 1000, 10000, 100000, 1000000, 1000_000_0, 1000_000_00, 1000_000_000})
            .register();

    int length;
    Integer id;
    String command;
    MessageStatus status = MessageStatus.Sending;
    MetaData metadata;
    byte[] data;
    //消息创建时间纳秒，决定了只能在本进程计算时间
    Long ts = System.nanoTime();

    //下一个消息，用于连续发送消息时候的处理
    Message next;
}
