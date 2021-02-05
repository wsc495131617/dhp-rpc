package org.chzcb.zk;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Aspect
@Component
@ConditionalOnBean(ZookeeperUtils.class)
public class ZKLockedAspect {

    @Resource
    ZookeeperUtils zookeeperUtils;

    @Pointcut(value = "@annotation(org.chzcb.zk.ZKLocked)")
    public void cutHasZKLocked() {
    }

    @Around(value = "cutHasZKLocked()")
    public Object invokeService(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getDeclaringTypeName() + "." + signature.getName();
        ZookeeperLock lock = zookeeperUtils.createLock(methodName.replace(".","_"));
        try {
            lock.lock();
            return joinPoint.proceed(joinPoint.getArgs());
        } finally {
            lock.unlock();
        }
    }

}
