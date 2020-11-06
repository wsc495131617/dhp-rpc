package org.dhp.net.nio;

import lombok.extern.slf4j.Slf4j;
import org.dhp.core.rpc.Session;
import org.dhp.core.rpc.SessionManager;

import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class NioSessionManager extends SessionManager {

    Map<SocketChannel, NioSession> allSessions = new ConcurrentHashMap<>();

    @Override
    public void forceClose() {

    }

    public Session getSession(Object socket) {
        if (allSessions.containsKey(socket)) {
            return allSessions.get(socket);
        }
        if (closing) {
            log.warn("It's closing, can't create session: {}", socket);
            return null;
        }
        SocketChannel channel = (SocketChannel) socket;
        NioSession session = new NioSession(channel);
        NioSession old = allSessions.putIfAbsent(channel, session);
        if (old != null) {
            session = old;
        }
        log.info("create session {}", session);
        return session;
    }

    @Override
    public void destorySession(Object socket) {

    }
}
