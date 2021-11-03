package org.dhp.net.netty4;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.dhp.core.rpc.MessageStatus;
import org.dhp.core.rpc.Session;
import org.dhp.core.rpc.SessionManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zhangcb
 */
@Slf4j
public class NettySessionManager extends SessionManager {

    Map<Channel, NettySession> allSessions = new ConcurrentHashMap<>();

    @Override
    public Session getSession(Object connection) {
        if (allSessions.containsKey(connection)) {
            return allSessions.get(connection);
        }
        if (closing) {
            log.warn("It's closing, can't create session: {}", connection);
            return null;
        }
        Channel channel = (Channel) connection;
        NettySession session = new NettySession(channel);
        NettySession old = allSessions.putIfAbsent(channel, session);
        if (old != null) {
            session = old;
        }
        log.info("create session {}", connection);
        register(session);
        return session;
    }

    @Override
    public void destorySession(Object connection) {
        NettySession session = allSessions.remove(connection);
        if (session != null) {
            session.destroy();
        }
    }

    @Override
    public void forceClose() {
        closing = true;
        log.info("call all sessions close message: {}", allSessions.size());
        allSessions.values().parallelStream().forEach(session -> {
            NettyMessage message = new NettyMessage();
            message.setId(0);
            message.setCommand("close");
            message.setStatus(MessageStatus.Completed);
            session.write(message);
            log.info("closing session: {}", session);
        });
        //同步等待session断开情况
        long st = System.currentTimeMillis();
        //最多等待5秒
        while (System.currentTimeMillis() - st < 5000) {
            if (allSessions.isEmpty()) {
                break;
            }
            try {
                Thread.sleep(100);
            } catch (Exception e) {
            }
        }
    }
}
