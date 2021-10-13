package org.dhp.core.rpc;

import io.prometheus.client.Gauge;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.dhp.common.rpc.SimpleStream;
import org.dhp.common.rpc.Stream;
import org.dhp.common.utils.NoServerIDGenerator;
import org.dhp.common.utils.ProtostuffUtils;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
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

    protected static Gauge rpcChannelPoolGuage = Gauge.build(
            "rpc_channel_pool_guage",
            "rpc连接池任务队列情况")
            .labelNames("name", "endpoint", "type")
            .register();

    String name;
    int port;
    String host;
    long timeout;
    ChannelType type;
    Long id;
    protected long activeTime = System.currentTimeMillis();
    protected boolean active;
    protected boolean isRegistering;

    protected static AtomicInteger _ID = new AtomicInteger(1);

    static NoServerIDGenerator channelID = new NoServerIDGenerator();

    //等待关闭的连接，有可能是网络延迟，或者服务端准备关闭的
    protected Set<Object> readyToCloseConns = ConcurrentHashMap.newKeySet();

    public RpcChannel() {
        id = channelID.make();
        //id = System.currentTimeMillis()*1000+ ThreadLocalRandom.current().nextInt(1000,9999);
    }

    protected boolean register() {
        this.isRegistering = true;
        byte[] idBytes = ProtostuffUtils.serialize(Long.class, this.getId());
        FutureImpl<Message> mfuture = new FutureImpl<>();
        Stream<Message> stream = new SimpleStream<Message>() {
            @Override
            public void onNext(Message value) {
                mfuture.result(value);
            }
        };
        mfuture.addStream(stream);
        write("register", idBytes, stream);
        Message resp = null;
        try {
            resp = mfuture.get(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("Register Failed by InterruptedException: " + getHost() + ":" + getPort() + " " + e.getMessage(), e);
        } catch (ExecutionException e) {
            log.warn("Register Failed by ExecutionException: " + getHost() + ":" + getPort() + " " + e.getMessage(), e);
        } catch (TimeoutException e) {
            log.warn("Register Failed by TimeoutException: " + getHost() + ":" + getPort() + " " + e.getMessage(), e);
        }
        if (resp != null && resp.getStatus() == MessageStatus.Completed) {
            this.active = true;
            this.isRegistering = false;
            return true;
        }
        this.isRegistering = false;
        return false;
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
        if (this.active == false && !this.isRegistering) {
            try {
                this.connect();
            } catch (TimeoutException e) {
                log.info("reconnect failed");
            }
            return;
        }
        Long ts = System.currentTimeMillis();
        byte[] idBytes = ProtostuffUtils.serialize(Long.class, ts);
        FutureImpl<Message> mfuture = new FutureImpl<>();
        Stream<Message> stream = new SimpleStream<Message>() {
            @Override
            public void onNext(Message value) {
                mfuture.result(value);
            }
        };
        mfuture.addStream(stream);
        write("ping", idBytes, stream);
        Message resp = null;
        try {
            resp = mfuture.get(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("Ping Failed by InterruptedException: " + getHost() + ":" + getPort() + " " + e.getMessage(), e);
        } catch (ExecutionException e) {
            log.warn("Ping Failed by ExecutionException: " + getHost() + ":" + getPort() + " " + e.getMessage(), e);
        } catch (TimeoutException e) {
            log.warn("Ping Failed by TimeoutException: " + getHost() + ":" + getPort() + " " + e.getMessage(), e);
        }
        if (resp != null && resp.getStatus() == MessageStatus.Completed) {
            this.active = true;
            this.activeTime = ts;
        } else {
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
