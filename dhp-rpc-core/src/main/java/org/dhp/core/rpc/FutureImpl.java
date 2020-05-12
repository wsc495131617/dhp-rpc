//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.dhp.core.rpc;

import lombok.extern.slf4j.Slf4j;
import org.dhp.common.rpc.Stream;
import org.dhp.common.rpc.StreamFuture;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

@Slf4j
public class FutureImpl<R> implements StreamFuture<R> {
    private final Object chSync = new Object();
    private Set<Stream<R>> streams;
    private final FutureImpl<R>.Sync sync = new FutureImpl.Sync();

    public void addStream(Stream<R> stream) {
        if (this.isDone()) {
            this.notifyStream(stream);
        } else {
            synchronized (this.chSync) {
                if (!this.isDone()) {
                    if (this.streams == null) {
                        this.streams = new HashSet(2);
                    }

                    this.streams.add(stream);
                    return;
                }
            }

            this.notifyStream(stream);
        }

    }

    public static <R> FutureImpl<R> create() {
        return new FutureImpl();
    }

    public FutureImpl() {
    }

    public void result(R result) {
        this.sync.innerSet(result);
    }

    public void failure(Throwable failure) {
        this.sync.innerSetException(failure);
    }

    public void markForRecycle(boolean recycleResult) {
    }

    public void recycle(boolean recycleResult) {
    }

    public void recycle() {
    }

    public R getResult() {
        if (this.isDone()) {
            try {
                return this.get();
            } catch (Throwable var2) {
            }
        }

        return null;
    }

    protected void onComplete() {
    }

    private void notifyStreams() {
        assert this.isDone();

        Set localSet;
        synchronized (this.chSync) {
            if (this.streams == null) {
                return;
            }

            localSet = this.streams;
            this.streams = null;
        }

        boolean isCancelled = this.isCancelled();
        R result = this.sync.result;
        Throwable error = this.sync.exception;
        Iterator it = localSet.iterator();

        while (it.hasNext()) {
            Stream<R> stream = (Stream) it.next();
            it.remove();

            try {
                if (isCancelled) {
                    stream.onCanceled();
                } else if (error != null) {
                    stream.onFailed(error);
                } else {
                    stream.onNext((R) result);
                    stream.onCompleted();
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }

    }

    private void notifyStream(Stream<R> stream) {
        if (this.isCancelled()) {
            stream.onCanceled();
        } else {
            try {
                Object result = this.get();
                try {
                    stream.onNext((R) result);
                    stream.onCompleted();
                } catch (Exception var4) {
                }
            } catch (ExecutionException var5) {
                stream.onFailed(var5.getCause());
            } catch (Exception var6) {
                stream.onFailed(var6);
            }
        }

    }

    public boolean isCancelled() {
        return this.sync.innerIsCancelled();
    }

    public boolean isDone() {
        return this.sync.ranOrCancelled();
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        return this.sync.innerCancel(mayInterruptIfRunning);
    }

    public R get() throws InterruptedException, ExecutionException {
        return this.sync.innerGet();
    }

    public R get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return this.sync.innerGet(unit.toNanos(timeout));
    }

    protected void done() {
        this.notifyStreams();
        this.onComplete();
    }

    private final class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = -7828117401763700385L;
        private static final int READY = 0;
        private static final int RESULT = 1;
        private static final int RAN = 2;
        private static final int CANCELLED = 3;
        private R result;
        private Throwable exception;

        private Sync() {
        }

        private boolean ranOrCancelled() {
            return (this.getState() & 3) != 0;
        }

        protected int tryAcquireShared(int ignore) {
            return this.ranOrCancelled() ? 1 : -1;
        }

        protected boolean tryReleaseShared(int ignore) {
            return true;
        }

        boolean innerIsCancelled() {
            return this.getState() == 3;
        }

        R innerGet() throws InterruptedException, ExecutionException {
            this.acquireSharedInterruptibly(0);
            if (this.getState() == 3) {
                throw new CancellationException();
            } else if (this.exception != null) {
                throw new ExecutionException(this.exception);
            } else {
                return this.result;
            }
        }

        R innerGet(long nanosTimeout) throws InterruptedException, ExecutionException, TimeoutException {
            if (!this.tryAcquireSharedNanos(0, nanosTimeout)) {
                throw new TimeoutException();
            } else if (this.getState() == 3) {
                throw new CancellationException();
            } else if (this.exception != null) {
                throw new ExecutionException(this.exception);
            } else {
                return this.result;
            }
        }

        void innerSet(R v) {
            if (this.compareAndSetState(0, 1)) {
                this.result = v;
                this.setState(2);
                this.releaseShared(0);
                FutureImpl.this.done();
            }

        }

        void innerSetException(Throwable t) {
            if (this.compareAndSetState(0, 1)) {
                this.exception = t;
                this.setState(2);
                this.releaseShared(0);
                FutureImpl.this.done();
            }

        }

        boolean innerCancel(boolean mayInterruptIfRunning) {
            if (this.compareAndSetState(0, 3)) {
                this.releaseShared(0);
                FutureImpl.this.done();
                return true;
            } else {
                return false;
            }
        }
    }
}
