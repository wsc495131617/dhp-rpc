package org.dhp.examples.rpcdemo.server;

import lombok.extern.slf4j.Slf4j;
import org.dhp.common.rpc.ListenableFuture;
import org.dhp.common.rpc.RpcResponse;
import org.dhp.common.rpc.Stream;
import org.dhp.core.rpc.FutureImpl;
import org.dhp.examples.rpcdemo.pojo.HelloRequest;
import org.dhp.examples.rpcdemo.pojo.HelloResponse;
import org.dhp.examples.rpcdemo.service.IHelloService;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class HelloServiceImpl implements IHelloService {

    ScheduledExecutorService pool = Executors.newScheduledThreadPool(2);

    AtomicInteger count = new AtomicInteger();

    @Override
    public HelloResponse say(HelloRequest request) {
        HelloResponse response = HelloResponse.builder()
                .content("say result")
                .build();
        return response;
    }

    public ListenableFuture<HelloResponse> asyncSay(HelloRequest request) {
        FutureImpl future = new FutureImpl<RpcResponse>() {
        };
        pool.schedule(() -> {
            log.info("asyncSay back");
            HelloResponse response = HelloResponse.builder()
                    .content("asyncSay result")
                    .build();
            future.result(response);
        }, 1000, TimeUnit.MILLISECONDS);
        return future;
    }

    public void streamSay(HelloRequest request, Stream<HelloResponse> stream) {
        pool.scheduleAtFixedRate(() -> {
            HelloResponse response = HelloResponse.builder()
                    .content("streamSay result: " + count.incrementAndGet())
                    .build();
            stream.onNext(response);
        }, 1000, 1000, TimeUnit.MILLISECONDS);
    }
}
