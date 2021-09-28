package org.chzcb.quartz;

import com.mchange.v1.db.sql.UnsupportedTypeException;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;

@Slf4j
public class QuartzAnnotationBeanPostProcessor implements BeanPostProcessor, ApplicationContextAware, ApplicationListener<ContextRefreshedEvent> {
    
    private static final String DEFAULT_GROUP = "AnnoQuartzDefault";
    
    private List<Trigger> triggers = new ArrayList<>();
    private List<JobDetail> jobDetails = new ArrayList<>();
    
    private ConfigurableApplicationContext applicationContext;
    
    private DefaultListableBeanFactory defaultListableBeanFactory;
    
    private Environment environment;
    
    @Resource
    QuartzJobConfiguration jobConfiguration;
    
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = (ConfigurableApplicationContext) applicationContext;
        defaultListableBeanFactory = (DefaultListableBeanFactory) this.applicationContext.getBeanFactory();
    }
    
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }
    
    /**
     * Bean加入之后，对
     *
     * @param bean
     * @param beanName
     * @return
     * @throws BeansException
     */
    public Object postProcessAfterInitialization(final Object bean, final String beanName) throws BeansException {
        //遍历方法，判断是否有Annotation为QuartzScheduled的方法
        Arrays.stream(bean.getClass().getDeclaredMethods()).filter(m -> m.getAnnotation(QuartzScheduled.class) != null).forEach(m -> {
            String clsName = bean.getClass().getName();
            String beanMethodName = m.getName();
            String jobBeanName = clsName + "." + beanMethodName + "JobDetail";
            String triggerBeanName = clsName + "." + beanMethodName + "Trigger";
            
            QuartzScheduled quartzScheduled = m.getAnnotation(QuartzScheduled.class);
            
            Class[] pClassArr = m.getParameterTypes();
            Object[] arguments = new Object[pClassArr.length];
            int hashCode = 0;
            if (arguments.length > 0) {
                for (QuartzJobConfiguration.JobDefine define : jobConfiguration.jobs) {
                    if (define.getName().equals(quartzScheduled.name())) {
                        arguments = new Object[pClassArr.length];
                        for (int i = 0; i < arguments.length; i++) {
                            try {
                                arguments[i] = getArgument(define.getArguments()[i], pClassArr[i]);
                                if (hashCode == 0) {
                                    hashCode = arguments[i].hashCode();
                                } else {
                                    hashCode = hashCode ^ arguments[i].hashCode();
                                }
                            } catch (UnsupportedTypeException e) {
                                log.error(e.getMessage(), e);
                            }
                        }
                        if (hashCode != 0) {
                            jobBeanName = jobBeanName + "-" + hashCode;
                            triggerBeanName = triggerBeanName + "-" + hashCode;
                        }
                        addJob(beanName, beanMethodName, arguments, pClassArr, quartzScheduled, jobBeanName, triggerBeanName);
                    }
                }
            } else {
                addJob(beanName, beanMethodName, arguments, pClassArr, quartzScheduled, jobBeanName, triggerBeanName);
            }
            
        });
        return bean;
    }
    
    private void addJob(String beanName, String beanMethodName, Object[] arguments, Class[] pClassArr, QuartzScheduled quartzScheduled, String jobBeanName, String triggerBeanName) {
        BeanDefinitionBuilder jobDetailBuilder = BeanDefinitionBuilder.genericBeanDefinition(JobDetailFactoryBean.class);
        jobDetailBuilder.addPropertyValue("durability", true);
        jobDetailBuilder.addPropertyValue("group", DEFAULT_GROUP);
        jobDetailBuilder.addPropertyValue("jobClass", quartzScheduled.cls());
        Map<String, Object> jobDataAsMap = new HashMap<>();
        jobDataAsMap.put("targetObject", beanName);
        jobDataAsMap.put("targetMethod", beanMethodName);
        jobDataAsMap.put("targetArguments", arguments);
        jobDataAsMap.put("targetMethodArgumentTypes", pClassArr);
        jobDetailBuilder.addPropertyValue("jobDataAsMap", jobDataAsMap);
        defaultListableBeanFactory.registerBeanDefinition(jobBeanName, jobDetailBuilder.getBeanDefinition());
        
        BeanDefinitionBuilder triggerBuilder = BeanDefinitionBuilder.genericBeanDefinition(CronTriggerFactoryBean.class);
        JobDetail jobDetail = applicationContext.getBean(jobBeanName, JobDetail.class);
        triggerBuilder.addPropertyValue("jobDetail", jobDetail);
        triggerBuilder.addPropertyValue("group", DEFAULT_GROUP);
        triggerBuilder.addPropertyValue("cronExpression", quartzScheduled.value());
        defaultListableBeanFactory.registerBeanDefinition(triggerBeanName, triggerBuilder.getBeanDefinition());
        
        
        triggers.add(applicationContext.getBean(triggerBeanName, Trigger.class));
        jobDetails.add(jobDetail);
    }
    
    /**
     * 获取入参的值
     *
     * @param arg
     * @param aClass
     * @return
     */
    private Object getArgument(String arg, Class aClass) throws UnsupportedTypeException {
        if(arg == null){
            throw new RuntimeException("need define arguments value in yaml or properties");
        }
        try {
            if (aClass == String.class) {
                return arg;
            } else if (aClass == Integer.class || aClass == int.class) {
                return Integer.valueOf(arg);
            } else if (aClass == BigDecimal.class) {
                return new BigDecimal(arg);
            } else if (aClass == Long.class || aClass == long.class) {
                return Long.valueOf(arg);
            } else if (aClass == Double.class || aClass == double.class) {
                return Double.valueOf(arg);
            } else {
                throw new UnsupportedTypeException();
            }
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            throw new UnsupportedTypeException();
        }
    }
    
    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        //
        if (contextRefreshedEvent.getApplicationContext() == this.applicationContext) {
            try {
                Scheduler scheduler = applicationContext.getBean(Scheduler.class);
                scheduler.start();
                Set<TriggerKey> keys = scheduler.getTriggerKeys(GroupMatcher.groupEquals(DEFAULT_GROUP));
                
                Map<String, TriggerKey> cacheMap = new HashMap<>();
                for (TriggerKey key : keys) {
                    cacheMap.put(key.getGroup() + "-" + key.getName(), key);
                }
                
                for (JobDetail jobDetail : jobDetails) {
                    scheduler.addJob(jobDetail, true);
                }
                
                for (Trigger trigger : triggers) {
                    if (!cacheMap.containsKey(trigger.getKey().getGroup() + "-" + trigger.getKey().getName())) {
                        scheduler.scheduleJob(trigger);
                    }
                }
            } catch (BeansException | SchedulerException e) {
                throw new RuntimeException(e);
            }
        }
    }
    
}
