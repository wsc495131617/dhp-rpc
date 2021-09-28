package org.dhp.core.rpc.cmd;

/**
 * Rpc命令
 */
public interface RpcCommand<REQ, RESP> {
    /**
     * 命令名称
     *
     * @return
     */
    String name();

    RESP execute(REQ arg);
}
