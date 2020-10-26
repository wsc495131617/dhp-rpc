package org.dhp.common.rpc;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RpcRequest implements IRpcRequest<RpcHeader>{
    RpcHeader header;
}
