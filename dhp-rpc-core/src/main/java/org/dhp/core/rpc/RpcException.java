package org.dhp.core.rpc;

import org.dhp.common.utils.StringFormatter;

import java.util.LinkedList;
import java.util.List;

/**
 * @author zhangcb
 */
public class RpcException extends RuntimeException {
    RpcErrorCode code;
    String[] params;

    public RpcException(RpcErrorCode code) {
        this.code = code;
    }

    public RpcException(RpcErrorCode code, String... args) {
        this(code);
        this.params = args;
    }

    public void setCode(RpcErrorCode code) {
        this.code = code;
    }

    public RpcErrorCode getCode() {
        return code;
    }

    public List<String> getResponse() {
        LinkedList<String> list = new LinkedList<>();
        list.add(code.name());
        if(params != null) {
            for (String param : params) {
                list.add(param);
            }
        }
        return list;
    }


    public String getMessage() {
        return StringFormatter.arrayFormat(code.getMessage(), params);
    }
}
