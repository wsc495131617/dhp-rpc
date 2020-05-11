package org.dhp.common.rpc;

public interface CompleteHandler<T> {
    void onCompleted(T object);

    void onCanceled();

    void onFailed(Throwable e);
}
