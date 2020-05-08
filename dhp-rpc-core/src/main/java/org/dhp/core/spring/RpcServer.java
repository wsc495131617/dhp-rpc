package org.dhp.core.spring;

import lombok.extern.slf4j.Slf4j;
import org.dhp.core.grizzly.GrizzlyRpcServer;
import org.dhp.core.netty4.NettyRpcServer;
import org.dhp.core.rpc.IRpcServer;
import org.dhp.core.rpc.RpcServerMethodManager;
import org.springframework.beans.factory.InitializingBean;

import javax.annotation.Resource;

@Slf4j
public class RpcServer implements InitializingBean {

    @Resource
    DhpProperties dhpProperties;

    @Resource
    RpcServerMethodManager methodManager;

    public void afterPropertiesSet() throws Exception {
        log.info("ready to start rpc server");
        if(dhpProperties.port <= 0 || dhpProperties.port > 65535){
            throw new FrameworkException("Invaild server port, must between 1-65535");
        }
        log.info("command count: {}", methodManager.getAllCommands().size());
        IRpcServer server;
        if(dhpProperties.type == "netty") {
            server = new NettyRpcServer(dhpProperties.port);
        } else {
            server = new GrizzlyRpcServer(dhpProperties.port);
        }
        server.start();
    }
}
