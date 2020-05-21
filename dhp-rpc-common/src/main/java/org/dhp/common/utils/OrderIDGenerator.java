package org.dhp.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * 
 * 增加服务器编号的配置
 * 支持时间戳为int的日期 2038-01-19 11:14:07
 * 11（空白）+1(增量位)+ 31（时间戳）+4（服务器ID）+ 17
 * 
 * @author zhangcb
 *
 */
public class OrderIDGenerator {
    static Logger logger = LoggerFactory.getLogger(OrderIDGenerator.class);
    
    protected long _baseID = 0;

    public long lastTimestamp;

    protected static int MaxID = 131071;

    protected String name;

    public OrderIDGenerator() {
        this.name = "Default";
    }

    // 向前调整过时间，那么就需要增加偏移量，保证就算回到以前的时间，也能够大概率不冲突
    long pre_diff = 0;

    /**
     * 
     * @param isClose
     * @return
     */
    public synchronized long make(boolean isClose) {
        long time = System.currentTimeMillis() / 1000;
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
            _baseID = 0;
        } else {
            if (_baseID >= MaxID) {
                try {
                    if (logger.isDebugEnabled())
                        logger.debug("wait for next millsecond");
                    Thread.sleep(1000);
                    return make(isClose);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        _baseID++;
        lastTimestamp = time;
        long id = _baseID + (time << 21)+(IDGenerator.SERVERID<<17);
        if (isClose)
            id = id + 0x10000000000000L;
        return id;
    }

    public String formatString(long id) {
        long pre = (id >> 52)&0x1;
        id = id & 0xFFFFFFFFFFFFFL;
        long time = id >> 21;
        long baseid = id & (0x1FFFFF);
        long serverid = baseid>>17;
        baseid = baseid & 0x1FFFF;
        return String.format("%s:time:%s,baseid:%d,serverid:%d", pre,
                StringFormat.formatDate(new Date(time * 1000)), baseid, serverid);
    }

    public static void main(String[] args) {
        IDGenerator.SERVERID = 10;
        OrderIDGenerator generator = new OrderIDGenerator();
        long lastTime = 0;
        long lastId = 0l;
        for(int i=0;i<10000000;i++) {
            long id = generator.make(true);
            long time = (id&0xFFFFFFFFFFFFFL) >> 21;
            if(lastTime != time) {
                System.out.println(generator.formatString(lastId) + "," + new Date());
                lastTime = time;
            }
            lastId= id;
        }
        
//        System.out.println(System.currentTimeMillis());
//        OrderIDGenerator.serverid = 10;
//        OrderIDGenerator generator = new OrderIDGenerator();
//        generator.lastTimestamp = System.currentTimeMillis() / 1000;
//        generator.lastTimestamp += 2;
//        long lastTime = 0;
//        long lastId = 0;
//        long time, baseid, lastBaseid;
//        for (int i = 0; i < 20; i++) {
//            long id = generator.make(false);
//            System.out.println(generator.formatString(id) + "," + new Date());
//            try {
//                if(i == 1)
//                generator.lastTimestamp = System.currentTimeMillis() / 1000+2;
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//            }
//        }
    }
}
