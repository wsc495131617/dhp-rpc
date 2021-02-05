package org.chzcb.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

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

    /**
     * 尝试设置，乐观锁机制
     *
     * @param key
     * @param value
     * @param oldValue
     * @return
     */
    public boolean trySet(final String key, final String value, final String oldValue) {
        Object ret = stringRedisTemplate.execute(new SessionCallback<Object>() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                List<Object> result = null;
                operations.watch(key);
                operations.multi();
                operations.opsForValue().get(key);
                operations.opsForValue().set(key, value);
                try {
                    result = operations.exec();
                    if (!result.isEmpty()) {
                        if ((oldValue == null && result.get(0) == null) || oldValue.equals(result.get(0))) {
                            return result;
                        } else {
                            log.warn("trySet key {} value from {} to {} failed, old {}", key, oldValue, value, result.get(0));
                        }
                        return null;
                    }
                } catch (Exception e) {
                    log.info("trySet warning", e);
                }
                return null;
            }
        });
        return ret != null;
    }
}
