package org.dhp.core.spring;

import lombok.extern.slf4j.Slf4j;
import org.dhp.common.annotation.DService;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cglib.proxy.Proxy;

@Slf4j
public class ClientProxyFactoryBean implements FactoryBean<Object>, InitializingBean, BeanFactoryAware {

    String className;

    Class<?> classType;

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
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
            classType = Class.forName(className);
            this.proxy = createProxy();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private Object createProxy() {
        ClientProxyInvokeHandler invocationHandler = beanFactory.getBean(ClientProxyInvokeHandler.class);
        Object proxy = Proxy.newProxyInstance(DService.class.getClassLoader(), new Class[]{classType}, invocationHandler);
        return proxy;
    }

    BeanFactory beanFactory;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }
}
