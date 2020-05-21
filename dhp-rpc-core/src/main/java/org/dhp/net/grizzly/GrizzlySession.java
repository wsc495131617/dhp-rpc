package org.dhp.net.grizzly;

import org.dhp.core.rpc.Message;
import org.dhp.core.rpc.Session;
import org.glassfish.grizzly.Connection;

public class GrizzlySession extends Session {
    
    Connection connection;
    
    public GrizzlySession(Connection connection){
        this.connection = connection;
    }
    
    @Override
    public void write(Message message) {
        this.connection.write(message);
    }
    
}
