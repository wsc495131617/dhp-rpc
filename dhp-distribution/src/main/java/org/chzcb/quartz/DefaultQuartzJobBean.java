package org.chzcb.quartz;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.quartz.QuartzJobBean;

import javax.annotation.Resource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Data
@Slf4j
public class DefaultQuartzJobBean extends QuartzJobBean {
    
    private String targetObject;
    
    private String targetMethod;
    
    private Class[] targetMethodArgumentTypes;
    
    private Object[] targetArguments;
    
    @Resource
    private ApplicationContext ctx;
    
    
    @Override
    protected void executeInternal(JobExecutionContext executionContext) throws JobExecutionException {
        try {
            Object curObj = this.ctx.getBean(getTargetObject());
            Method m = null;
            try{
                m = curObj.getClass().getMethod(targetMethod, targetMethodArgumentTypes);
                m.invoke(curObj, targetArguments);
            } catch (NoSuchMethodException e) {
                log.info("NoSuchMethodException: "+e.getMessage(), e);
            } catch (IllegalAccessException e) {
                log.info("IllegalAccessException: "+e.getMessage(), e);
            } catch (InvocationTargetException e) {
                log.info("InvocationTargetException: "+e.getMessage(), e);
            }
        } catch (Exception e){
            throw new JobExecutionException(e);
        }
    }
}
