package org.dhp.core.rpc;

public abstract class Session {
    public abstract void write(Message message);
}
