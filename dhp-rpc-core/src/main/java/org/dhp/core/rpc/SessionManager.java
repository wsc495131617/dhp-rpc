package org.dhp.core.rpc;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务端SessionManager，给channel
 */
@Slf4j
public abstract class SessionManager {
    
    protected boolean isClosing = false;
    
    Map<Long, Session> sessions = new ConcurrentHashMap<>();
    
    public boolean register(Session session) {
        Session old = sessions.putIfAbsent(session.getId(), session);
        return old == null;
    }
    
    public Session getSessionById(Long sessionId) {
        return sessions.get(sessionId);
    }
    
    public void destory(Session session) {
        Long id = session.getId();
        if(id != null)
            this.sessions.remove(id);
    }
    
    /**
     * 强制关闭客户端，不让后续请求访问进来
     */
    public abstract void forceClose();
    
    public abstract Session getSession(Object connection);
    
    public abstract void destorySession(Object connection);
}
