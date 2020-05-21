package org.dhp.common.utils;

import org.apache.commons.lang3.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public class NoServerIDGenerator {
    static Logger logger = LoggerFactory.getLogger(NoServerIDGenerator.class);

    protected long _baseID = 0;
    protected long lastTimestamp;

    protected static int MaxID = 65535;

    public NoServerIDGenerator() {
        _baseID = RandomUtils.nextInt(1, 10000);
    }


    // 向前调整过时间，那么就需要增加偏移量，保证就算回到以前的时间，也能够大概率不冲突
    long pre_diff = 0;

    public synchronized long make() {
        long time = System.currentTimeMillis();
        if (lastTimestamp > time) {
            pre_diff = lastTimestamp - time;
        } else if (pre_diff > 0) {
            pre_diff += lastTimestamp - time - 1;
            if (pre_diff < 0) {
                pre_diff = 0;
            }
        }
        if (pre_diff > 0)
            time += pre_diff;
        if (time > lastTimestamp && lastTimestamp > 0) {
            if (logger.isDebugEnabled())
                logger.debug("last create id:{},at:{}", _baseID, lastTimestamp);
            _baseID = RandomUtils.nextInt(1, 10000);
        } else {
            if (_baseID >= MaxID) {
                try {
                    if (logger.isDebugEnabled())
                        logger.debug("wait for next millsecond");
                    Thread.sleep(1);
                    return make();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        _baseID++;
        lastTimestamp = time;
        return 0x7FFFFFFFFFFFFFFFL & (_baseID + (time << 16));
    }

    public String formatString(long id) {
        long time = (id >> 16) & 0x3FFFFFFFFFFl;
        long baseid = id & (0xff);
        return String.format("time:%s,baseid:%d",
                StringFormat.formatDate(new Date(time)), baseid);
    }

    public static long getTime(long id) {
        return (id >> 16) & 0x3FFFFFFFFFFl;
    }
}
