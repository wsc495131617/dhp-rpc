package org.dhp.core.rpc.cmd;

import lombok.Data;
import org.dhp.core.rpc.ServerCommand;

/**
 * @author zhangcb
 */
@Data
public class ServerRpcCommand extends ServerCommand {
    Class<?> reqCls;
    Class<?> respCls;
}
