package org.dhp.common.rpc;

/**
 * 基础请求
 */
public abstract class RpcRequest<H extends RpcHeader> {
    public abstract void setHeader(H header);
    public abstract H getHeader();
}
