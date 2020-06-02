package org.dhp.core.spring;

import lombok.extern.slf4j.Slf4j;
import org.dhp.common.annotation.DService;
import org.dhp.core.rpc.RpcServerMethodManager;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;

import javax.annotation.Resource;

/**
 * @author zhangcb
 */
@Slf4j
public class DhpServerRegister implements BeanPostProcessor, ResourceLoaderAware, BeanFactoryAware, EnvironmentAware, BeanClassLoaderAware {

    private ResourceLoader resourceLoader;

    private Environment environment;

    private ClassLoader classLoader;

    private BeanFactory beanFactory;

    @Resource
    RpcServerMethodManager methodManager;
    
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
