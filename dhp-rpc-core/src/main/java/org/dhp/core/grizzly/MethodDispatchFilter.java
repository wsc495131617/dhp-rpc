package org.dhp.core.grizzly;

import org.dhp.core.rpc.RpcServerMethodManager;
import org.dhp.core.rpc.ServerCommand;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class MethodDispatchFilter extends BaseFilter {
    RpcServerMethodManager methodManager;

    public MethodDispatchFilter(RpcServerMethodManager methodManager) {
        this.methodManager = methodManager;
    }

    public NextAction handleRead(FilterChainContext ctx) throws IOException {
        GrizzlyMessage message = ctx.getMessage();
        ServerCommand command = methodManager.getCommand(message.getCommand());
        Object[] args = getCommandArgs(message);
        try {
            Object result = command.getMethod().invoke(command.getBean(), args);
            GrizzlyMessage retMessage = new GrizzlyMessage();
            retMessage.setCommand(command.getName());
            ctx.getConnection().write(retMessage);
        } catch (IllegalAccessException e) {

        } catch (InvocationTargetException e) {
        }
        return ctx.getStopAction();
    }

    private Object[] getCommandArgs(GrizzlyMessage message) {
        return new Object[]{};
    }
}
