package org.dhp.examples.rpcdemo.pojo;

import lombok.Builder;
import lombok.Data;
import org.dhp.common.rpc.RpcResponse;

@Data
@Builder
public class HelloResponse extends RpcResponse {
    String content;
}
