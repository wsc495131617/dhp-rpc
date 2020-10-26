package org.dhp.common.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 基础RPC请求的响应结果
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RpcResponse implements IRpcResponse{

    @JsonIgnoreProperties
    //uuid 用于幂等
    Long uuid;


    int code = 200;
    String message;
}
