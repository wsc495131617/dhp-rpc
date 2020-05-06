package org.dhp.core.spring;

import lombok.extern.slf4j.Slf4j;
import org.dhp.common.annotation.DService;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.cglib.proxy.InvocationHandler;
import org.springframework.cglib.proxy.Proxy;
import org.springframework.core.type.AnnotationMetadata;

/**
 * 客户端代理工厂，用于创建代理bean
 */
@Slf4j
public class ClientProxyFactory {

    public Object createProxy(AnnotatedBeanDefinition annotatedBeanDefinition){
        try {
            AnnotationMetadata annotationMetadata = annotatedBeanDefinition.getMetadata();
            Class<?> target = Class.forName(annotationMetadata.getClassName());
            InvocationHandler invocationHandler = new ClientProxyInvokeHandler();
            Object proxy = Proxy.newProxyInstance(DService.class.getClassLoader(), new Class[]{target}, invocationHandler);
            return proxy;
        } catch (ClassNotFoundException e) {
            log.error(e.getMessage());
        }
        return null;
    }

}
