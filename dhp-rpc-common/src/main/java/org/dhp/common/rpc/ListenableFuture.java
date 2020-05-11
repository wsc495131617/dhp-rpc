package org.dhp.common.rpc;

import java.util.concurrent.Future;

public interface ListenableFuture<T> extends Future<T> {
    void addCompleteHandler(CompleteHandler<T> handler);
}
