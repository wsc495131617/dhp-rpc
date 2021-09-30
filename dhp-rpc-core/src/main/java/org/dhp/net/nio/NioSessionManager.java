package org.dhp.net.nio;

import lombok.extern.slf4j.Slf4j;
import org.dhp.core.rpc.Session;
import org.dhp.core.rpc.SessionManager;

@Slf4j
public class NioSessionManager extends SessionManager {

    @Override
    public void forceClose() {

    }

    public Session getSession(Object socket) {
        return null;
    }

    @Override
    public void destorySession(Object socket) {

    }
}
