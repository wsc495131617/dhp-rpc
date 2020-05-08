package org.dhp.examples.rpcdemo.client;

import org.dhp.common.annotation.DMethod;
import org.dhp.common.annotation.DService;
import org.dhp.common.rpc.RpcResponse;
import org.dhp.core.rpc.Stream;
import org.dhp.examples.rpcdemo.pojo.HelloRequest;

import java.util.concurrent.Future;

@DService
public interface IHelloService {

    @DMethod(command = "hello/say")
    public RpcResponse say(HelloRequest request);

    @DMethod(command = "hello/asyncSay")
    public Future<RpcResponse> asyncSay(HelloRequest request);

    @DMethod(command = "hello/streamSay")
    public void streamSay(HelloRequest request, Stream<RpcResponse> stream);
}
