//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.dhp.core.rpc;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

public class FutureImpl<R> implements ListenableFuture<R> {
    private final Object chSync = new Object();
    private Set<CompleteHandler<R>> CompleteHandlers;
    private final FutureImpl<R>.Sync sync = new FutureImpl.Sync();

    public void addCompleteHandler(CompleteHandler<R> CompleteHandler) {
        if (this.isDone()) {
            this.notifyCompleteHandler(CompleteHandler);
        } else {
            synchronized(this.chSync) {
                if (!this.isDone()) {
                    if (this.CompleteHandlers == null) {
                        this.CompleteHandlers = new HashSet(2);
                    }

                    this.CompleteHandlers.add(CompleteHandler);
                    return;
                }
            }

            this.notifyCompleteHandler(CompleteHandler);
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

    private void notifyCompleteHandlers() {
        assert this.isDone();

        Set localSet;
        synchronized(this.chSync) {
            if (this.CompleteHandlers == null) {
                return;
            }

            localSet = this.CompleteHandlers;
            this.CompleteHandlers = null;
        }

        boolean isCancelled = this.isCancelled();
        R result = this.sync.result;
        Throwable error = this.sync.exception;
        Iterator it = localSet.iterator();

        while(it.hasNext()) {
            CompleteHandler<R> CompleteHandler = (CompleteHandler)it.next();
            it.remove();

            try {
                if (isCancelled) {
                    CompleteHandler.onCanceled();
                } else if (error != null) {
                    CompleteHandler.onFailed(error);
                } else {
                    CompleteHandler.onCompleted(result);
                }
            } catch (Exception var8) {
            }
        }

    }

    private void notifyCompleteHandler(CompleteHandler<R> CompleteHandler) {
        if (this.isCancelled()) {
            CompleteHandler.onCanceled();
        } else {
            try {
                Object result = this.get();

                try {
                    CompleteHandler.onCompleted((R)result);
                } catch (Exception var4) {
                }
            } catch (ExecutionException var5) {
                CompleteHandler.onFailed(var5.getCause());
            } catch (Exception var6) {
                CompleteHandler.onFailed(var6);
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
        this.notifyCompleteHandlers();
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
