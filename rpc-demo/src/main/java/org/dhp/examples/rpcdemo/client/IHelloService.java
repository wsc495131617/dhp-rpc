package org.dhp.examples.rpcdemo.client;

import org.dhp.common.annotation.DMethod;
import org.dhp.common.annotation.DService;
import org.dhp.core.rpc.ListenableFuture;
import org.dhp.core.rpc.Stream;
import org.dhp.examples.rpcdemo.pojo.HelloRequest;
import org.dhp.examples.rpcdemo.pojo.HelloResponse;

@DService
public interface IHelloService {

    @DMethod(command = "hello/say")
    public HelloResponse say(HelloRequest request);

    @DMethod(command = "hello/asyncSay")
    public ListenableFuture<HelloResponse> asyncSay(HelloRequest request);

    @DMethod(command = "hello/streamSay")
    public void streamSay(HelloRequest request, Stream<HelloResponse> stream);
}
