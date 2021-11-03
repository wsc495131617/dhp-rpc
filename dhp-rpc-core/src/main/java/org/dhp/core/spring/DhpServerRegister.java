package org.dhp.core.spring;

import lombok.extern.slf4j.Slf4j;
import org.dhp.common.annotation.DService;
import org.dhp.core.rpc.ChannelType;
import org.dhp.core.rpc.IRpcServer;
import org.dhp.core.rpc.RpcServerMethodManager;
import org.dhp.core.rpc.cmd.RpcCommand;
import org.dhp.net.grizzly.GrizzlyRpcServer;
import org.dhp.net.netty4.NettyRpcServer;
import org.dhp.net.nio.NioRpcSocketServer;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;

import javax.annotation.Resource;

/**
 * @author zhangcb
 */
@Slf4j
@Order(1)
@ConditionalOnProperty(prefix = "dhp", name = "port")
public class DhpServerRegister implements BeanPostProcessor, DisposableBean, ApplicationRunner {

    @Resource
    RpcServerMethodManager methodManager;

    @Resource
    DhpProperties dhpProperties;

    IRpcServer server;

    @Override
    public void destroy() throws Exception {
        log.info("RpcServer({}), stopping, waiting for 1 seconds!", dhpProperties.getPort());
        server.shutdown();
        Thread.sleep(1000);
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?>[] clslist = bean.getClass().getInterfaces();
        if (bean instanceof Advised) {
            clslist = ((Advised) bean).getTargetClass().getInterfaces();
        }
        Class<?> interCls = null;
        Class<?> cmdCls = null;
        for (Class<?> cls : clslist) {
            if (RpcCommand.class.isAssignableFrom(cls)) {
                cmdCls = cls;
                break;
            }
            DService an = cls.getAnnotation(DService.class);
            if (an != null) {
                interCls = cls;
                break;
            }
        }
        if (interCls != null) {
            methodManager.addServiceBean(bean, interCls);
        }
        if (cmdCls != null) {
            methodManager.addCommand((RpcCommand) bean);
        }
        return bean;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (dhpProperties.port <= 0) {
            throw new FrameworkException("Invaild server port");
        }
        if (server == null) {
            if (dhpProperties.type == ChannelType.Netty) {
                server = new NettyRpcServer(dhpProperties.port, dhpProperties.getWorkThread());
            } else if (dhpProperties.type == ChannelType.NIO) {
                server = new NioRpcSocketServer(dhpProperties.getPort(), dhpProperties.getWorkThread());
            } else {
                server = new GrizzlyRpcServer(dhpProperties.port, dhpProperties.getWorkThread());
            }
            methodManager.initBasicCommand();
            server.start(methodManager);
            log.info("RpcServer({}) started!", dhpProperties.getPort());
        }
    }
}
