package org.dhp.common.rpc;

public interface Stream<T> {

    void onCanceled();

    void onNext(T value);

    void onFailed(Throwable throwable);

    void onCompleted();
}
