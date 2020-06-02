package org.dhp.core.rpc;

import lombok.extern.slf4j.Slf4j;

import javax.security.auth.Destroyable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务端SessionManager，给channel
 * @author zhangcb
 */
@Slf4j
public abstract class SessionManager implements Destroyable {
    
    protected boolean closing = false;
    
    public boolean isClosing(){
        return this.closing;
    }
    
    Map<Long, Session> sessions = new ConcurrentHashMap<>();
    
    public boolean register(Session session) {
        Session old = sessions.putIfAbsent(session.getId(), session);
        return old == null;
    }
    
    public Session getSessionById(Long sessionId) {
        return sessions.get(sessionId);
    }
    
    /**
     * 销毁
     * @param session
     */
    public void destroy(Session session) {
        Long id = session.getId();
        if(id != null) {
            this.sessions.remove(id);
        }
    }
    
    /**
     * 强制关闭客户端，不让后续请求访问进来
     */
    public abstract void forceClose();
    
    /**
     * 通过connection获得Session
     * @param connection
     * @return
     */
    public abstract Session getSession(Object connection);
    
    /**
     * 通过connection注销Session
     * @param connection
     */
    public abstract void destorySession(Object connection);
}
