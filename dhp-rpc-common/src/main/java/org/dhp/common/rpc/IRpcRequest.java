package org.dhp.common.rpc;

/**
 * 基础请求
 */
public interface IRpcRequest<H extends IRpcHeader> {
    void setHeader(H header);
    H getHeader();
}
