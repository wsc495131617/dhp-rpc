package org.dhp.core.rpc;

public enum MessageStatus {
    Sending(0),Completed(1),Updating(2), Canceled(3), Failed(4);
    int id;
    MessageStatus(int id){
        this.id = id;
    }
    public int getId() {
        return id;
    }
}
