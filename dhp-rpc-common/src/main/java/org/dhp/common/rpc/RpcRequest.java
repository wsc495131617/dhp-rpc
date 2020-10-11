package org.dhp.common.rpc;

import lombok.Data;

/**
 * 基础请求
 */
@Data
public class RpcRequest<H extends RpcHeader> {
    H header;
}
