package org.dhp.core.rpc;

public interface IMethodManager {
    void addServiceBean(Object bean, Class<?> cls);
    ServerCommand getCommand(String command);
}
