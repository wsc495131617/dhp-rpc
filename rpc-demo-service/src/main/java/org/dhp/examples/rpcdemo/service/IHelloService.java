package org.dhp.examples.rpcdemo.service;

import org.dhp.examples.rpcdemo.pojo.HelloRequest;
import org.dhp.examples.rpcdemo.pojo.HelloResponse;
import org.dhp.common.annotation.DMethod;
import org.dhp.common.annotation.DService;
import org.dhp.common.rpc.ListenableFuture;
import org.dhp.common.rpc.Stream;

@DService
public interface IHelloService {

    @DMethod(command = "hello/say")
    public HelloResponse say(HelloRequest request);

    @DMethod(command = "hello/asyncSay")
    public ListenableFuture<HelloResponse> asyncSay(HelloRequest request);

    @DMethod(command = "hello/streamSay")
    public void streamSay(HelloRequest request, Stream<HelloResponse> stream);
}
