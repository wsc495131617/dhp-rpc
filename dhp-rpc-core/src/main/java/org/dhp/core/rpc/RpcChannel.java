package org.dhp.core.rpc;

import io.prometheus.client.Gauge;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.dhp.common.rpc.Stream;
import org.dhp.common.utils.NoServerIDGenerator;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rpc 通道，用于发送信息
 * <p>
 * 对于断开重连的尝试，假设channel在心跳间隔时间内重连上，那么channel继续使用
 *
 * @author zhangcb
 */
@Slf4j
@Data
public abstract class RpcChannel {

    String name;
    int port;
    String host;
    long timeout;
    ChannelType type;
    Long id;
    protected long activeTime = System.currentTimeMillis();
    protected boolean active;

    protected static AtomicInteger _ID = new AtomicInteger(1);

    static NoServerIDGenerator channelID = new NoServerIDGenerator();

    //等待关闭的连接，有可能是网络延迟，或者服务端准备关闭的
    protected Set<Object> readyToCloseConns = ConcurrentHashMap.newKeySet();

    public RpcChannel() {
        id = channelID.make();
    }

    /**
     * start channel
     */
    public abstract void start();

    /**
     * ping channel with connected
     */
    public void ping() {
        //如果连接应不活跃，那么ping就没必要了，需要重新连接
        if (this.active == false) {
            try {
                this.connect();
            } catch (TimeoutException e) {
                log.info("reconnect failed");
            }
            return;
        }
        try {
            RpcCaller.call(this, "ping", System.currentTimeMillis(), Long.class);
            this.active = true;
            this.activeTime = System.currentTimeMillis();
        } catch (Exception e) {
            this.active = false;
        }
    }

    /**
     * is close(different from active)
     */
    public abstract boolean isClose();

    /**
     * connect to server
     *
     * @return
     * @throws TimeoutException
     */
    public abstract boolean connect() throws TimeoutException;

    /**
     * write message to server
     *
     * @param name
     * @param argBody
     * @param stream
     * @return
     */
    public abstract Integer write(String name, byte[] argBody, Stream<Message> stream);

    public abstract void close();
}
