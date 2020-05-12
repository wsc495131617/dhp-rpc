package org.dhp.common.rpc;

import java.util.concurrent.Future;

public interface StreamFuture<T> extends Future<T> {
    void addStream(Stream<T> stream);
}
