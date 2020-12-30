package org.dhp.core;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.dhp.common.rpc.RpcResponse;
import org.dhp.core.rpc.ChannelType;
import org.dhp.core.rpc.RpcChannel;
import org.dhp.core.rpc.RpcChannelBuilder;
import org.dhp.core.rpc.RpcServerMethodManager;
import org.dhp.net.nio.NioRpcSocketServer;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

@Slf4j
public class TestNioChannel {


    @Data
    @Builder
    private static class HelloReq {
        String name;
    }

    private static interface TestService {
        RpcResponse callHello(HelloReq req);
    }

    private static class TestServiceImpl implements TestService {
        @Override
        public RpcResponse callHello(HelloReq req) {
            return RpcResponse.builder().code(1).message(req.getName()).build();
        }
    }


    TestServiceImpl service;

    RpcServerMethodManager methodManager;

    int port;

    @Before
    public void initBeans() {
        methodManager = new RpcServerMethodManager();
        port = RandomUtils.nextInt(10000, 13000);

        //处理method
//        methodManager.addServiceBean();
    }

    @Test
    public void createServer() throws IOException {
        NioRpcSocketServer server = new NioRpcSocketServer(port, 4);
        server.start(methodManager);
    }

    @Test
    public void testChannel() {
        RpcChannel channel = new RpcChannelBuilder()
                .setHost("localhost")
                .setPort(port)
                .setName("testnio"+port)
                .setTimeout(3000)
                .setType(ChannelType.NIO)
                .build();
        channel.start();
        for(int i=0;i<10;i++){
        }
    }

}
