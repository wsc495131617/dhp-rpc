package org.dhp.core.rpc;

public interface CompleteHandler<T> {
    void onCompleted(T object);
    void onCanceled();
    void onFailed(Throwable e);
}
