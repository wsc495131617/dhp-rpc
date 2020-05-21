package org.dhp.core.rpc;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务端SessionManager，给channel
 */
public abstract class SessionManager {
    
    Map<Long, Session> sessions = new ConcurrentHashMap<>();
    
    public boolean register(Session session) {
        Session old = sessions.putIfAbsent(session.getId(), session);
        return old == null;
    }
    
    public Session getSessionById(Long sessionId) {
        return sessions.get(sessionId);
    }
    
    public void destory(Session session){
        this.sessions.remove(session.getId());
    }
    
    public abstract Session getSession(Object connection);
    public abstract void destorySession(Object connection);
}
