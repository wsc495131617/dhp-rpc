package org.chzcb.quartz;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class JobInfo {
    String triggerName;
    String triggerGroup;
    String jobName;
    String jobGroup;
    String cron;
    Long preFireTime;
    Long nextFireTime;
    String triggerState;
    Long startTime;
}
