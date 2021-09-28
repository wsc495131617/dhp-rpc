package org.dhp.core.spring;

import lombok.extern.slf4j.Slf4j;
import org.dhp.common.annotation.DService;
import org.dhp.core.rpc.RpcChannelPool;
import org.dhp.lb.NodeCenter;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.*;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
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
public class DhpClientRegister implements ImportBeanDefinitionRegistrar,
        EnvironmentAware, BeanClassLoaderAware {

    private Environment environment;

    private ClassLoader classLoader;

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry, BeanNameGenerator importBeanNameGenerator) {
        Map<String, Object> annotationAttributes = importingClassMetadata.getAnnotationAttributes(DhpRpcClientScanner.class.getName());
        ClassPathScanningCandidateComponentProvider scanner = getClassScanner();
        String[] basePackages = null;
        if (annotationAttributes != null) {
            basePackages = (String[]) annotationAttributes.get("basePackages");
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
                } catch (Exception e) {

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

                        BeanDefinition rbd = BeanDefinitionBuilder.genericBeanDefinition(ClientProxyFactoryBean.class)
                                .addPropertyValue("className", compDefinition.getBeanClassName())
                                .setAutowireMode(GenericBeanDefinition.AUTOWIRE_BY_TYPE)
                                .getBeanDefinition();
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
        //添加ClientProxyInvokeHandler
        BeanDefinition invokeBeanDefinition = BeanDefinitionBuilder.rootBeanDefinition(ClientProxyInvokeHandler.class).getBeanDefinition();
        registry.registerBeanDefinition(invokeBeanDefinition.getBeanClassName(), invokeBeanDefinition);

        //添加RpcChannelPool
        BeanDefinition poolBeanDefinition = BeanDefinitionBuilder.rootBeanDefinition(RpcChannelPool.class).getBeanDefinition();

        if("true".equalsIgnoreCase(environment.getProperty("dhp.lb.enable"))) {
            //添加NodeCenter
            BeanDefinition nodeCenterBeanDefinition = BeanDefinitionBuilder.rootBeanDefinition(NodeCenter.class).getBeanDefinition();
            registry.registerBeanDefinition(nodeCenterBeanDefinition.getBeanClassName(), nodeCenterBeanDefinition);
            poolBeanDefinition.setDependsOn(nodeCenterBeanDefinition.getBeanClassName());
        }

        registry.registerBeanDefinition(poolBeanDefinition.getBeanClassName(), poolBeanDefinition);
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
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }


    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

}
