package org.dhp.core.spring.boot;

import lombok.extern.slf4j.Slf4j;
import org.dhp.core.spring.DhpProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableConfigurationProperties(DhpProperties.class)
@ConditionalOnProperty(name = "dhp")
public class DhpAutoConfiguration {

}
