package org.chzcb.redis;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RedisLock {
    String lockName;
    String lockValue;
    LockUtils lockUtils;
    public boolean release() {
        return lockUtils.releaseLock(lockName, lockValue);
    }
}
