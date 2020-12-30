package org.dhp.core.rpc;

import lombok.extern.slf4j.Slf4j;
import org.dhp.common.rpc.Stream;
import org.dhp.common.rpc.StreamFuture;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zhangcb
 */
@Slf4j
public abstract class Session {
    protected Long id;

    public boolean isRegister() {
        return id != null;
    }

    public void setId(Long value) {
        this.id = value;
    }

    public Long getId() {
        return id;
    }

    protected AtomicInteger frameCount = new AtomicInteger(0);
    protected long frameTime;

    protected long incrementCount() {
        long lastFrameTime = this.frameTime;
        this.frameTime = System.currentTimeMillis() / 1000;
        if (this.frameTime == lastFrameTime) {
            return this.frameCount.incrementAndGet();
        } else {
            return this.frameCount.getAndSet(1);
        }
    }

    public abstract void write(Message message);

    Set<Stream> streams = ConcurrentHashMap.newKeySet();

    Set<StreamFuture> futures = ConcurrentHashMap.newKeySet();

    public void addStream(Stream stream) {
        streams.add(stream);
    }


    public void addFuture(StreamFuture future) {
        futures.add(future);
    }

    public void destroy() {
        streams.stream().forEach(stream -> {
            stream.onCanceled();
        });
        streams.clear();
        futures.stream().forEach(streamFuture -> {
            streamFuture.cancel(false);
        });
        futures.clear();
        log.info("create session {}", this);
    }
}
