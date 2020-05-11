package com.example.webdemo;

import org.dhp.core.spring.DhpProperties;
import org.dhp.core.spring.EnableDhpRpcClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(DhpProperties.class)
@EnableDhpRpcClient(basePackages="org.dhp.examples.rpcdemo")
public class WebDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebDemoApplication.class, args);
    }

}
