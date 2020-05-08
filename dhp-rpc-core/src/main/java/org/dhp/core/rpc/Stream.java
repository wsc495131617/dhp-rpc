package org.dhp.core.rpc;

public interface Stream<T> {
    void onNext(T value);

    void onError(Throwable throwable);

    void onCompleted();
}
