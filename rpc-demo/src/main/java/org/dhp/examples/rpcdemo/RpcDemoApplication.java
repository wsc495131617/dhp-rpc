package org.dhp.examples.rpcdemo;

import org.dhp.core.spring.EnableDhpRpcClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "org.dhp")
@EnableDhpRpcClient
public class RpcDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(RpcDemoApplication.class, args);
    }

}
