package org.dhp.core.rpc;

import org.dhp.common.utils.StringFormatter;

/**
 * @author zhangcb
 */
public class RpcException extends RuntimeException {
    RpcErrorCode code;
    String[] params;

    public RpcException(){
    }

    public void setCode(RpcErrorCode code) {
        this.code = code;
    }

    public RpcException(RpcErrorCode code) {
        super(code.name());
        this.code = code;
    }

    public RpcException(RpcErrorCode code, String...args) {
        this(code);
        this.params = args;
    }
    
    public RpcErrorCode getCode() {
        return code;
    }

    public String getMessage() {
        return StringFormatter.arrayFormat(code.getMessage(), params);
    }
}
