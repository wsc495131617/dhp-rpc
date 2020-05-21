package org.dhp.net.netty4;

import io.netty.channel.Channel;
import org.dhp.core.rpc.Session;
import org.dhp.core.rpc.SessionManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NettySessionManager extends SessionManager {
    
    Map<Channel, NettySession> allSessions = new ConcurrentHashMap<>();
    
    public Session getSession(Object connection) {
        if(allSessions.containsKey(connection)){
            return allSessions.get(connection);
        }
        Channel channel = (Channel)connection;
        NettySession session = new NettySession(channel);
        NettySession old = allSessions.putIfAbsent(channel, session);
        if(old != null ){
            session = old;
        }
        return session;
    }
    
    public void destorySession(Object connection) {
        NettySession session = allSessions.remove(connection);
        if(session != null){
            session.destory();
        }
    }
}
