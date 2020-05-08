package org.dhp.examples.rpcdemo.server;

import org.dhp.common.rpc.RpcResponse;
import org.dhp.core.rpc.Stream;
import org.dhp.examples.rpcdemo.client.IHelloService;
import org.dhp.examples.rpcdemo.pojo.HelloRequest;
import org.springframework.stereotype.Service;

import java.util.concurrent.Future;

@Service
public class HelloServiceImpl implements IHelloService {

    @Override
    public RpcResponse say(HelloRequest request) {
        return null;
    }

    @Override
    public Future<RpcResponse> asyncSay(HelloRequest request) {
        return null;
    }

    @Override
    public void streamSay(HelloRequest request, Stream<RpcResponse> stream) {

    }
}
