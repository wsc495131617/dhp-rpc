package org.dhp.core.rpc;

import lombok.Data;

/**
 * @author zhangcb
 */
@Data
public class RpcFailedResponse {
    String clsName;
    String message;
    String content;
}
