package org.dhp.core.grizzly;

import org.dhp.core.rpc.Session;
import org.dhp.core.rpc.SessionManager;
import org.glassfish.grizzly.Connection;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GrizzlySessionManager extends SessionManager {
    Map<Connection, GrizzlySession> allSessions = new ConcurrentHashMap<>();
    
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
        return session;
    }
    
    public void destorySession(Object connection) {
        allSessions.remove(connection);
    }
}
