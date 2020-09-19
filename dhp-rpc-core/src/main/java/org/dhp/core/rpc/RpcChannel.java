package org.dhp.core.rpc;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.dhp.common.rpc.SimpleStream;
import org.dhp.common.rpc.Stream;
import org.dhp.common.utils.ProtostuffUtils;

import java.util.Set;
import java.util.concurrent.*;

/**
 * Rpc 通道，用于发送信息
 *
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
    
    //等待关闭的连接，有可能是网络延迟，或者服务端准备关闭的
    protected Set<Object> readyToCloseConns = ConcurrentHashMap.newKeySet();
    
    public RpcChannel(){
        id = System.currentTimeMillis()*10000000+ ThreadLocalRandom.current().nextInt(1000000,9999999);
    }

    protected boolean register(){
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
            log.warn("Register Failed by InterruptedException: "+getHost()+":"+getPort()+" "+e.getMessage(), e);
        } catch (ExecutionException e) {
            log.warn("Register Failed by ExecutionException: "+getHost()+":"+getPort()+" "+e.getMessage(), e);
        } catch (TimeoutException e) {
            log.warn("Register Failed by TimeoutException: "+getHost()+":"+getPort()+" "+e.getMessage(), e);
        }
        if (resp != null && resp.getStatus() == MessageStatus.Completed) {
            this.active = true;
            return true;
        }
        return false;
    }

    /**
     * start channel
     */
    public abstract void start();
    
    /**
     * ping channel with connected
     */
    public abstract void ping();

    /**
     * is close(different from active)
     */
    public abstract boolean isClose();
    
    /**
     * connect to server
     * @return
     * @throws TimeoutException
     */
    public abstract boolean connect() throws TimeoutException;

    /**
     * write message to server
     * @param name
     * @param argBody
     * @param stream
     * @return
     */
    public abstract Integer write(String name, byte[] argBody, Stream<Message> stream);

    public abstract void close();
}
