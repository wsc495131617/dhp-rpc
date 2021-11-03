package org.dhp.net.grizzly;

import lombok.extern.slf4j.Slf4j;
import org.dhp.core.rpc.MessageStatus;
import org.dhp.core.rpc.Session;
import org.dhp.core.rpc.SessionManager;
import org.glassfish.grizzly.Connection;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zhangcb
 */
@Slf4j
public class GrizzlySessionManager extends SessionManager {
    Map<Connection, GrizzlySession> allSessions = new ConcurrentHashMap<>();
    
    @Override
    public Session getSession(Object connection) {
        if (allSessions.containsKey(connection)) {
            return allSessions.get(connection);
        }
        Connection channel = (Connection) connection;
        GrizzlySession session = new GrizzlySession(channel);
        GrizzlySession old = allSessions.putIfAbsent(channel, session);
        if (old != null) {
            session = old;
        }
        log.info("create session {}", connection);
        register(session);
        return session;
    }
    
    @Override
    public void destorySession(Object connection) {
        GrizzlySession session = allSessions.remove(connection);
        if(session != null){
            this.destroy(session);
            session.destroy();
        }
    }
    
    @Override
    public void forceClose() {
        closing = true;
        allSessions.values().parallelStream().forEach(session -> {
            GrizzlyMessage message = new GrizzlyMessage();
            message.setId(0);
            message.setCommand("close");
            message.setStatus(MessageStatus.Completed);
            session.write(message);
            log.info("closing session: {}", session);
        });
    }
}
