package org.dhp.core.rpc;

/**
 * 消息状态
 * @author zhangcb
 */
public enum MessageStatus {
    Sending(0), Completed(1), Updating(2), Canceled(3), Failed(4),Timeout(5);
    int id;

    MessageStatus(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
