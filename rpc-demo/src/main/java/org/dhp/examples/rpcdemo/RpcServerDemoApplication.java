package org.dhp.examples.rpcdemo;

import org.dhp.core.spring.DhpProperties;
import org.dhp.core.spring.EnableDhpRpcServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = {"org.dhp.examples.rpcdemo"})
@EnableConfigurationProperties(DhpProperties.class)
@EnableDhpRpcServer
public class RpcServerDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(RpcServerDemoApplication.class, args);
    }

}
