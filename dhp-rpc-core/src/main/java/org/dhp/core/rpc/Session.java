package org.dhp.core.rpc;

public abstract class Session {
    protected Long id;
    public boolean isRegister() {
        return id != null;
    }
    public void setId(Long value) {
        this.id = value;
    }
    public Long getId() {
        return id;
    }
    
    public abstract void write(Message message);
}
