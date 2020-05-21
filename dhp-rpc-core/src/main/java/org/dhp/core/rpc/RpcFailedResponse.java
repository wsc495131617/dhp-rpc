package org.dhp.core.rpc;

import lombok.Data;

@Data
public class RpcFailedResponse {
    String clsName;
    String message;
    String content;
}
