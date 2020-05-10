package org.dhp.examples.rpcdemo;

import lombok.extern.slf4j.Slf4j;
import org.dhp.core.rpc.Stream;
import org.dhp.examples.rpcdemo.client.IHelloService;
import org.dhp.examples.rpcdemo.pojo.HelloRequest;
import org.dhp.examples.rpcdemo.pojo.HelloResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Slf4j
@SpringBootTest(classes = {RpcClientDemoApplication.class})
class RpcClientDemoApplicationTests {

    @Resource
    IHelloService service;

    int TOTAL = 50000;

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
        HelloResponse ret = service.say(new HelloRequest());
        log.info("default say: {}", ret);
        Future<HelloResponse> responseFuture = service.asyncSay(new HelloRequest());
        try {
            ret = responseFuture.get();
            log.info("future asyncSay: {}", ret);
        } catch (InterruptedException e) {
        } catch (ExecutionException e) {
        }
        log.info("default say: {}", ret);
        service.streamSay(new HelloRequest(), new Stream<HelloResponse>() {
            public void onCanceled() {
                log.info("onCancel stream");
            }
            public void onNext(HelloResponse value) {
                log.info("onNext stream: {}", value);
            }
            public void onFailed(Throwable throwable) {
                log.info("onError stream: {}", throwable);
            }
            public void onCompleted() {
                log.info("onCompleted stream");
            }
        });
    }

}
