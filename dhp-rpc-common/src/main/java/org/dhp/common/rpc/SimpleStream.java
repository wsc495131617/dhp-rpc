package org.dhp.common.rpc;

public class SimpleStream<T> implements Stream<T> {
    public void onCanceled() {
    }
    public void onNext(T value) {
    }
    public void onFailed(Throwable throwable) {
    }
    public void onCompleted() {
    }
}
