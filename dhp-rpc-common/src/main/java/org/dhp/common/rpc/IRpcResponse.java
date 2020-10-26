package org.dhp.common.rpc;

public interface IRpcResponse {
    void setCode(int code);
    int getCode();
    void setMessage(String message);
    String getMessage();
}
