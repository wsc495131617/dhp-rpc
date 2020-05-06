package org.dhp.examples.rpcdemo.client;

import org.dhp.common.annotation.DMethod;
import org.dhp.common.annotation.DService;
import org.dhp.common.rpc.RpcResponse;
import org.dhp.examples.rpcdemo.pojo.HelloRequest;

@DService
public interface IHelloService {

    @DMethod(commandId = 1001001)
    public RpcResponse say(HelloRequest request);
}
