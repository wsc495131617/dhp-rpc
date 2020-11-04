package org.dhp.core.spring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import java.lang.reflect.Proxy;

@Slf4j
public class ClientProxyFactoryBean<T> implements FactoryBean<T>, InitializingBean, BeanFactoryAware {

    String className;

    public void setClassName(String className) {
        this.className = className;
    }

    Class classType;

    T proxy;

    @Override
    public T getObject() throws Exception {
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

    private T createBean() {
        IClientInvokeHandler invocationHandler = beanFactory.getBean(IClientInvokeHandler.class);
        T proxy = (T) Proxy.newProxyInstance(invocationHandler.getClass().getClassLoader(), new Class<?>[]{classType}, invocationHandler);
        return proxy;
    }

    BeanFactory beanFactory;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }
}
