package org.dhp.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * 
 * 不保证服务器之间
 * 
 * @author zhangcb
 *
 */
public class JSLongGenerator {
static Logger logger = LoggerFactory.getLogger(IDGenerator.class);
	
	protected long _baseID = 0;
	
	protected long lastTimestamp;
	
	protected static int MaxID = 4194303;
	
	protected String name;
	
	public JSLongGenerator() {
		this.name = "Default";
	}
	
	//向前调整过时间，那么就需要增加偏移量，保证就算回到以前的时间，也能够大概率不冲突
	long pre_diff = 0;
    
	public synchronized long make() {
	    long time = System.currentTimeMillis()/1000;
	    if(lastTimestamp>time){
            pre_diff = lastTimestamp-time;
        } else if(pre_diff>0){
            pre_diff += lastTimestamp-time-1;
            if(pre_diff<0) {
                pre_diff = 0;
            }
        }
        if(pre_diff>0)
            time += pre_diff;
        
        if(time>lastTimestamp && lastTimestamp>0) {
            if(logger.isDebugEnabled())
                logger.debug("last create id:{},at:{}",_baseID,lastTimestamp);
            _baseID = 0;
        }
        else
        {
            if(_baseID>=MaxID){
                try {
                    if(logger.isDebugEnabled())
                        logger.debug("wait for next millsecond");
                    Thread.sleep(1000);
                    return make();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        _baseID++;
        lastTimestamp = time;
        long id = _baseID+(time<<22);
        return id; 
	}
	
	public String formatString(long id){
		long time = id>>22;
		long baseid = id&(0x3fffff);
		return String.format("%s:time:%s,baseid:%d", name, StringFormat.formatDate(new Date(time*1000),"YYYY-MM-dd HH:mm:ss SSS"),baseid);
	}
}
