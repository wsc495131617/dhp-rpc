package org.dhp.examples.rpcdemo;

import lombok.extern.slf4j.Slf4j;
import org.dhp.common.rpc.RpcResponse;
import org.dhp.core.rpc.Stream;
import org.dhp.examples.rpcdemo.client.IHelloService;
import org.dhp.examples.rpcdemo.pojo.HelloRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.concurrent.Future;

@Slf4j
@SpringBootTest
class RpcClientDemoApplicationTests {

    @Resource
    IHelloService service;

    int TOTAL = 30000000;

    @Test
    void contextLoads() {
        service.say(new HelloRequest());
        long st = System.currentTimeMillis();
        for(int i=0;i<TOTAL;i++) {
            service.say(new HelloRequest());
        }
        log.info("cost {} ms", (System.currentTimeMillis()-st));
    }

    @Test
    void callHello() {
        service.say(new HelloRequest());
        Future<RpcResponse> responseFuture = service.asyncSay(new HelloRequest());
        service.streamSay(new HelloRequest(), new Stream<RpcResponse>() {
            public void onNext(RpcResponse value) {
            }
            public void onError(Throwable throwable) {
            }
            public void onCompleted() {

            }
        });
    }

}
