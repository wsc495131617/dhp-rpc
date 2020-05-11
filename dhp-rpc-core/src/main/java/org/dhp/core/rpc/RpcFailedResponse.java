package org.dhp.core.rpc;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RpcFailedResponse {
    String content;
    String clsName;
    String message;
}
