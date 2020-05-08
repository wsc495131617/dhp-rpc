package org.dhp.examples.rpcdemo;

import org.dhp.core.spring.DhpProperties;
import org.dhp.core.spring.EnableDhpRpcClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = "org.dhp.examples.rpcdemo.client")
@EnableConfigurationProperties(DhpProperties.class)
@EnableDhpRpcClient
public class RpcClientDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(RpcClientDemoApplication.class, args);
    }

}
