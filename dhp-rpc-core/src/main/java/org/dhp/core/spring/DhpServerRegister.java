package org.dhp.core.spring;

import lombok.extern.slf4j.Slf4j;
import org.dhp.common.annotation.DService;
import org.dhp.core.rpc.ChannelType;
import org.dhp.core.rpc.IRpcServer;
import org.dhp.core.rpc.RpcServerMethodManager;
import org.dhp.net.grizzly.GrizzlyRpcServer;
import org.dhp.net.netty4.NettyRpcServer;
import org.dhp.net.nio.NioRpcSocketServer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;

import javax.annotation.Resource;

/**
 * @author zhangcb
 */
@Slf4j
@ConditionalOnProperty(prefix = "dhp", name = "port")
public class DhpServerRegister implements BeanPostProcessor, ResourceLoaderAware, BeanFactoryAware, EnvironmentAware, BeanClassLoaderAware, InitializingBean, DisposableBean {

    private ResourceLoader resourceLoader;

    private Environment environment;

    private ClassLoader classLoader;

    private BeanFactory beanFactory;

    @Resource
    RpcServerMethodManager methodManager;

    @Resource
    DhpProperties dhpProperties;

    IRpcServer server;

    @Override
    public void afterPropertiesSet() throws Exception {
        if (dhpProperties.port <= 0) {
            throw new FrameworkException("Invaild server port");
        }
        if (server == null) {
            if (dhpProperties.type == ChannelType.Netty) {
                server = new NettyRpcServer(dhpProperties.port, dhpProperties.getWorkThread());
            } else if(dhpProperties.type == ChannelType.NIO){
                server = new NioRpcSocketServer(dhpProperties.getPort(),dhpProperties.getWorkThread(), methodManager);
            } else {
                server = new GrizzlyRpcServer(dhpProperties.port, dhpProperties.getWorkThread());
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
    
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        Class<?>[] clslist = bean.getClass().getInterfaces();
        Class<?> interCls = null;
        for (Class<?> cls : clslist) {
            DService an = cls.getAnnotation(DService.class);
            if (an != null) {
                interCls = cls;
                break;
            }
        }
        if (interCls != null) {
            methodManager.addServiceBean(bean, interCls);
        }
        return bean;
    }
    
    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }


    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }
    
}
