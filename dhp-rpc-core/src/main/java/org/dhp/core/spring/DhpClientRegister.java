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
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;

import java.util.Map;
import java.util.Set;

@Slf4j
public class DhpClientRegister implements ImportBeanDefinitionRegistrar, ResourceLoaderAware, BeanFactoryAware, EnvironmentAware, BeanClassLoaderAware {

    private ResourceLoader resourceLoader;

    private Environment environment;

    private ClassLoader classLoader;

    private BeanFactory beanFactory;

    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        Map<String, Object> annotationAttributes = importingClassMetadata.getAnnotationAttributes(DService.class.getName());
        ClassPathScanningCandidateComponentProvider scanner = getClassScanner();
        String[] basePackages = null;
        if(annotationAttributes != null ){
            basePackages = (String[])annotationAttributes.get("basePackages");
        }
        if (basePackages == null || basePackages.length == 0) {//PrintServiceScan的basePackages默认为空数组
            String basePackage = null;
            try {
                basePackage = Class.forName(importingClassMetadata.getClassName()).getPackage().getName();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            basePackages = new String[] {basePackage};
        }
        AnnotationTypeFilter filter = new AnnotationTypeFilter(DService.class);
        scanner.addIncludeFilter(filter);
        for(String basePackage : basePackages) {
            Set<BeanDefinition> comps = scanner.findCandidateComponents(basePackage);
            for(BeanDefinition compDefinition : comps){
                try {
                    if(compDefinition instanceof AnnotatedBeanDefinition){
                        RootBeanDefinition rbd = new RootBeanDefinition(ClientProxyFactoryBean.class);
                        MutablePropertyValues propertyValues = new MutablePropertyValues();
                        propertyValues.add("className", compDefinition.getBeanClassName());
                        rbd.setPropertyValues(propertyValues);
                        ((BeanDefinitionRegistry) this.beanFactory).registerBeanDefinition("proxy_"+compDefinition.getBeanClassName(), rbd);
                    } else {
                        log.debug("skip component definition: {}", compDefinition);
                    }
                } catch (Throwable e){
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
                if (beanDefinition.getMetadata().isInterface()) {
                    try {
                        Class<?> target = ClassUtils.forName(
                                beanDefinition.getMetadata().getClassName(),
                                classLoader);
                        return !target.isAnnotation();
                    } catch (Exception ex) {
                        log.error("load class exception:", ex);
                    }
                }
                return false;
            }
        };
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
