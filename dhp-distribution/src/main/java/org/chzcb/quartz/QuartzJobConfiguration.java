package org.chzcb.quartz;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "quartz")
public class QuartzJobConfiguration {
    List<JobDefine> jobs;
    @Data
    public static class JobDefine {
        String name;
        String[] arguments;
    }
}
