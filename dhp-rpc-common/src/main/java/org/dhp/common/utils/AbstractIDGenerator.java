package org.dhp.common.utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractIDGenerator {

    protected long maxId = 1024;
    protected long sequenceID = 0;
    protected long lastTs = 0;

    //向前调整过时间，那么就需要增加偏移量，保证就算回到以前的时间，也能够大概率不冲突
    long pre_diff = 0;

    protected void increment() {
        long time;
        do {
            time = System.currentTimeMillis()/1000;
            if (lastTs > time) {
                pre_diff = lastTs - time;
            } else if (pre_diff > 0) {
                pre_diff += lastTs - time - 1;
                if (pre_diff < 0) {
                    pre_diff = 0;
                }
            }
            if (pre_diff > 0)
                time += pre_diff;

            if (time > lastTs && lastTs > 0) {
                if (log.isDebugEnabled())
                    log.debug("last create id:{},at:{}", sequenceID, lastTs);
                sequenceID = 0;
            } else {
                if (sequenceID >= maxId) {
                    try {
                        if (log.isDebugEnabled())
                            log.debug("wait for next millsecond");
                        Thread.sleep(1);
                        continue;
                    } catch (InterruptedException e) {
                        log.warn("");
                    }
                } else {
                    sequenceID++;
                }
            }
        } while (sequenceID>maxId);
        lastTs = time;
    }

    public abstract long make();
}
