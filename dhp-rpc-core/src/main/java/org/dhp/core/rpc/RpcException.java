package org.dhp.core.rpc;

public class RpcException extends RuntimeException {
    public RpcException(RpcErrorCode code) {
        super(code.name());
    }
}
