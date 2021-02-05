package org.chzcb.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * 通过Redis实现的锁
 * 1. 乐观锁
 * 2. 悲观锁，颗粒度比较小的业务可以使用，确定的或者颗粒度大的业务可以通过ZKLocked或者ZookeeperUtils来实现
 */
@Component
@ConditionalOnClass(StringRedisTemplate.class)
@Slf4j
public class LockUtils {

    @Resource
    StringRedisTemplate stringRedisTemplate;

    private DefaultRedisScript<Long> tryDelLuaScript;

    private DefaultRedisScript<String> trySetLuaScript;

    @PostConstruct
    public void init() {
        trySetLuaScript = new DefaultRedisScript<>();
        trySetLuaScript.setResultType(String.class);
        trySetLuaScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/try_set.lua")));

        tryDelLuaScript = new DefaultRedisScript<>();
        tryDelLuaScript.setResultType(Long.class);
        tryDelLuaScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/try_del.lua")));
    }

    /**
     * 尝试设置，乐观锁机制
     *
     * @param key
     * @param value
     * @param oldValue
     * @return
     */
    public boolean trySet(String key, final String value, final String oldValue) {
        String ret = stringRedisTemplate.execute(trySetLuaScript, Collections.singletonList(key), value, oldValue);
        return "OK".equals(ret);
    }


    /**
     *
     * @param lockKey
     * @return
     */
    public RedisLock tryLock(String lockKey) {
        return tryLock(lockKey, 5000);
    }
    /**
     * 尝试悲观锁，并返回锁值
     *
     * @param lockKey
     * @param expire  锁超时时间，正常业务处理最坏的情况也不允许超过锁超时时间，如果业务处理时间不可控，那么请用zk锁
     * @return
     */
    public RedisLock tryLock(String lockKey, long expire) {
        String lockValue = String.valueOf(System.currentTimeMillis());
        RedisLock lock = RedisLock.builder()
                .lockValue(lockValue)
                .lockName(lockKey)
                .lockUtils(this)
                .build();
        if (!stringRedisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, expire, TimeUnit.MILLISECONDS)) {
            return null;
        }
        return lock;
    }

    /**
     * 释放锁
     *
     * @param lockKey
     * @param lockValue
     * @return
     */
    public boolean releaseLock(String lockKey, String lockValue) {
        return 1l == tryDel(lockKey, lockValue);
    }

    /**
     * 尝试删除，确保值就是老的才能删除
     *
     * @param key
     * @param value
     * @return
     */
    public Long tryDel(String key, String value) {
        return stringRedisTemplate.execute(tryDelLuaScript, Collections.singletonList(key), value);
    }
}
