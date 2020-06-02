package org.dhp.core.spring;

import lombok.extern.slf4j.Slf4j;
import org.dhp.core.rpc.ChannelType;
import org.dhp.core.rpc.IRpcServer;
import org.dhp.core.rpc.RpcServerMethodManager;
import org.dhp.net.grizzly.GrizzlyRpcServer;
import org.dhp.net.netty4.NettyRpcServer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import javax.annotation.Resource;

/**
 * @author zhangcb
 */
@Slf4j
public class RpcServer implements InitializingBean, DisposableBean {

    @Resource
    DhpProperties dhpProperties;

    @Resource
    RpcServerMethodManager methodManager;
    
    IRpcServer server;

    @Override
    public void afterPropertiesSet() throws Exception {
        if (dhpProperties.port <= 0) {
            throw new FrameworkException("Invaild server port");
        }
        if (server == null) {
            if (dhpProperties.type == ChannelType.Netty) {
                server = new NettyRpcServer(dhpProperties.port);
            } else {
                server = new GrizzlyRpcServer(dhpProperties.port);
            }
            server.start(methodManager);
            log.info("RpcServer({}) started!", dhpProperties.getPort());
        }
    }
    
    @Override
    public void destroy() throws Exception {
        log.info("RpcServer({}), stopping, waiting for 1 seconds!", dhpProperties.getPort());
        server.shutdown();
        Thread.sleep(1000);
    }
}
