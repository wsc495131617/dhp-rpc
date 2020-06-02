package org.dhp.core.spring;

import lombok.extern.slf4j.Slf4j;
import org.dhp.common.annotation.DService;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author zhangcb
 */
@Slf4j
public class DhpClientRegister implements ImportBeanDefinitionRegistrar, ApplicationContextAware,
        BeanFactoryAware, EnvironmentAware, BeanClassLoaderAware, PriorityOrdered {
    
    private Environment environment;
    
    private ClassLoader classLoader;
    
    private ApplicationContext applicationContext;
    
    private BeanFactory beanFactory;
    
    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry, BeanNameGenerator importBeanNameGenerator) {
        Map<String, Object> annotationAttributes = importingClassMetadata.getAnnotationAttributes(EnableDhpRpcClient.class.getName());
        Map<String, Object> middleAttributes = importingClassMetadata.getAnnotationAttributes(EnableDhpRpcMiddle.class.getName());
        ClassPathScanningCandidateComponentProvider scanner = getClassScanner();
        String[] basePackages = null;
        if (annotationAttributes != null) {
            basePackages = (String[]) annotationAttributes.get("basePackages");
        }
        if(middleAttributes != null){
            basePackages = (String[]) middleAttributes.get("basePackages");
        }
        //PrintServiceScan的basePackages默认为空数组
        if (basePackages == null || basePackages.length == 0) {
            String basePackage = null;
            try {
                basePackage = Class.forName(importingClassMetadata.getClassName()).getPackage().getName();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            basePackages = new String[]{basePackage};
        }
        AnnotationTypeFilter filter = new AnnotationTypeFilter(DService.class, false, true);
        scanner.addIncludeFilter(filter);
        for (String basePackage : basePackages) {
            Set<BeanDefinition> comps = scanner.findCandidateComponents(basePackage);
            //需要过滤，有Bean实现的接口
            Set<Class> impledClassSet = new HashSet<>();
            for (BeanDefinition compDefinition : comps) {
                try {
                    //class
                    Class cls = Class.forName(compDefinition.getBeanClassName());
                    //如果不是接口
                    if (!cls.isInterface()) {
                        impledClassSet.add(cls);
                    }
                } catch (Exception e){
                
                }
            }
            for (BeanDefinition compDefinition : comps) {
                try {
                    if (compDefinition instanceof AnnotatedBeanDefinition) {
                        //class
                        Class cls = classLoader.loadClass(compDefinition.getBeanClassName());
    
                        boolean isImpled = false;
                        for (Class impledCls : impledClassSet) {
                            if (impledCls == cls || cls.isAssignableFrom(impledCls)) {
                                isImpled = true;
                                break;
                            }
                        }
                        if (isImpled) {
                            continue;
                        }
                        
                        RootBeanDefinition rbd = new RootBeanDefinition(ClientProxyFactoryBean.class);
                        MutablePropertyValues propertyValues = new MutablePropertyValues();
                        propertyValues.add("className", compDefinition.getBeanClassName());
                        rbd.setPropertyValues(propertyValues);
                        log.info("add dhp-proxy: {}", cls.getName());
                        registry.registerBeanDefinition("dhp-"+cls.getName(), rbd);
                    } else {
                        log.debug("skip component definition: {}", compDefinition);
                    }
                } catch (Throwable e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
        //添加Bean
        BeanDefinition invokeBeanDefinition = BeanDefinitionBuilder.rootBeanDefinition(ClientProxyInvokeHandler.class).getBeanDefinition();
        registry.registerBeanDefinition(invokeBeanDefinition.getBeanClassName(), invokeBeanDefinition);
    }
    
    
    private ClassPathScanningCandidateComponentProvider getClassScanner() {
        return new ClassPathScanningCandidateComponentProvider(false, this.environment) {
            
            @Override
            protected boolean isCandidateComponent(
                    AnnotatedBeanDefinition beanDefinition) {
                try {
                    Class<?> target = ClassUtils.forName(
                            beanDefinition.getMetadata().getClassName(),
                            classLoader);
                    return !target.isAnnotation();
                } catch (Exception ex) {
                    log.error("load class exception:", ex);
                }
                return false;
            }
        };
    }
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
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
    
    @Override
    public int getOrder() {
        return Integer.MIN_VALUE;
    }
}
