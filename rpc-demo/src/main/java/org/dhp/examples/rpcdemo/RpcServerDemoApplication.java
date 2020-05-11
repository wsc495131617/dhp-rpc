package org.dhp.examples.rpcdemo;

import org.dhp.core.spring.DhpProperties;
import org.dhp.core.spring.EnableDhpRpcServer;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = {"org.dhp.examples.rpcdemo.server"})
@EnableConfigurationProperties(DhpProperties.class)
@EnableDhpRpcServer
public class RpcServerDemoApplication implements CommandLineRunner {

    public static void main(String[] args) {
        new SpringApplicationBuilder(RpcServerDemoApplication.class).web(WebApplicationType.NONE).run(args);
    }

    @Override
    public void run(String... args) throws Exception {
        System.in.read();
    }
}
