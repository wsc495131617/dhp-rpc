package org.dhp.lb;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Ha事件，一般用于主从
 */
public class HaEvent extends ApplicationEvent {

    /**
     * 手动放弃主的身份事件
     */
    public static final String GIVE_UP_MASTER = "give_up_master";

    /**
     * 准备成为主，这时候老的主已经放弃，准备成为主的允许接收消息，但是需要等新主完成准备，才进行处理，因此可以客户端请求等待
     * 为什么不放到服务端等待，是因为发送过程不可控，不可预知消息的中间状态
     */
    public static final String PREPARE_MASTER = "prepare_master";

    /**
     * 成为
     */
    public static final String TOBE_MASTER = "tobe_master";

    /**
     * 竞争失败，需要通知从终止竞争
     */
    public static final String PREEMPT_MASTER_FAILED = "preempt_master_failed";


    @Getter
    protected String event;

    public HaEvent(String event, Object source) {
        super(source);
        this.event = event;
    }
}
