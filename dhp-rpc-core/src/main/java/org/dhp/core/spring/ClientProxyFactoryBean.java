package org.dhp.core.spring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cglib.proxy.Proxy;

@Slf4j
public class ClientProxyFactoryBean implements FactoryBean<Object>, InitializingBean, BeanFactoryAware {

    String className;

    public void setClassName(String className) {
        this.className = className;
    }

    Class classType;

    Object proxy;

    String getBean() {
        return "dhp-" + className;
    }

    @Override
    public Object getObject() throws Exception {
        return proxy;
    }

    @Override
    public Class<?> getObjectType() {
        return classType;
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.classType = Class.forName(className);
        this.proxy = createBean();
    }

    private Object createBean() {
        IClientInvokeHandler invocationHandler = beanFactory.getBean(IClientInvokeHandler.class);
        Object proxy = Proxy.newProxyInstance(invocationHandler.getClass().getClassLoader(), new Class<?>[]{classType}, invocationHandler);
        return proxy;
    }

    BeanFactory beanFactory;

    ProxyFactoryBean proxyFactoryBean;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }
}
