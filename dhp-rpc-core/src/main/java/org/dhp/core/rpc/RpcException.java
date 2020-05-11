package org.dhp.core.rpc;

public class RpcException extends RuntimeException {
    public RpcException(ErrorCode code) {
        super(code.name());
    }
}
