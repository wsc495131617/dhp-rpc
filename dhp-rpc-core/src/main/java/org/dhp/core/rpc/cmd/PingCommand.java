package org.dhp.core.rpc.cmd;

public class PingCommand implements RpcCommand<Long, Long> {

    @Override
    public String name() {
        return "ping";
    }

    @Override
    public Long execute(Long id) {
        return System.currentTimeMillis();
    }
}
