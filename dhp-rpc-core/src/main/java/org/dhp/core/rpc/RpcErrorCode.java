package org.dhp.core.rpc;

/**
 * Rpc error code define
 * @author zhangcb
 */
public enum RpcErrorCode {
    COMMAND_NOT_FOUND(1000, "Command is not found, check method definition"),
    NODE_NOT_FOUND(1001, "Node is not found, check node configuration"),
    ILLEGAL_PARAMETER_DEFINITION(1002, "Illegal parameter definition"),
    SEND_MESSAGE_FAILED(1003, "Send message failed"),
    UNREACHABLE_NODE(1004, "Unreachable node"),
    UNKNOWN_EXEPTION(1005, "unknow excption"),
    TIMEOUT(1006, "timeout");

    private RpcErrorCode(int code, String msg) {
        this.code = code;
        this.message = msg;
    }

    int code;
    String message;

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
