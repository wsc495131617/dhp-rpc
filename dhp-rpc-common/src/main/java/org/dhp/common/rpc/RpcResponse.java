package org.dhp.common.rpc;

import lombok.Data;

/**
 * 基础RPC请求的响应结果
 */
@Data
public class RpcResponse {
    //uuid 用于幂等
    Long uuid;
    int code;
    String message;
}
