package org.dhp.core.spring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import java.lang.reflect.Proxy;

@Slf4j
public class ClientProxyFactoryBean implements FactoryBean<Object>, InitializingBean, BeanFactoryAware {

    Class<?> classType;
    
    public void setClassType(Class<?> classType) {
        this.classType = classType;
    }
    
    public Class<?> getClassType() {
        return classType;
    }
    
    Object proxy;

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
        return true;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            this.proxy = createProxy();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private Object createProxy() {
        ClientProxyInvokeHandler invocationHandler = beanFactory.getBean(ClientProxyInvokeHandler.class);
        Object proxy = Proxy.newProxyInstance(invocationHandler.getClass().getClassLoader(), new Class<?>[]{classType}, invocationHandler);
        return proxy;
    }

    BeanFactory beanFactory;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }
}
