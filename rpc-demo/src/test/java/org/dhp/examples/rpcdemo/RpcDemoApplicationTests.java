package org.dhp.examples.rpcdemo;

import lombok.extern.slf4j.Slf4j;
import org.dhp.examples.rpcdemo.client.IHelloService;
import org.dhp.examples.rpcdemo.pojo.HelloRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@Slf4j
@SpringBootTest
class RpcDemoApplicationTests {

    @Resource
    IHelloService service;

    int TOTAL = 100000;

    @Test
    void contextLoads() {
        long st = System.nanoTime();
        for(int i=0;i<TOTAL;i++) {
            service.say(new HelloRequest());
        }
        log.info("{} pre second", TOTAL*1000000000/(System.nanoTime()-st));
    }

}
