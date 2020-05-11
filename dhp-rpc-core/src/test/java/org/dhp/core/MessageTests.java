package org.dhp.core;

import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;
import org.dhp.core.grizzly.GrizzlyMessage;
import org.dhp.core.netty4.NettyMessage;
import org.dhp.core.rpc.MetaData;
import org.glassfish.grizzly.Buffer;
import org.junit.Test;

@Slf4j
public class MessageTests {
    int TOTAL = 3000000;

    @Test
    public void grizzlyMessage(){
        GrizzlyMessage msg = new GrizzlyMessage();
        msg.setId(1);
        msg.setCommand("test");
        MetaData metadata = new MetaData();
        metadata.add(2, "ddd");
        msg.setMetadata(metadata);
        msg.setData("hello".getBytes());
        Buffer buffer = msg.pack();
        log.info("buffer:{}", buffer);
        msg = new GrizzlyMessage(buffer);
        log.info("msg:{}", msg);

        msg = new GrizzlyMessage();
        msg.setId(1);
        msg.setCommand("test");
        msg.setData("hello".getBytes());
        buffer = msg.pack();
        log.info("buffer:{}", buffer);
        msg = new GrizzlyMessage(buffer);
        log.info("msg:{}", msg);

        long st = System.currentTimeMillis();
        for(int i=0;i<TOTAL;i++){
            msg = new GrizzlyMessage();
            msg.setId(1);
            msg.setCommand("test");
            metadata = new MetaData();
            metadata.add(2, "ddd");
            msg.setMetadata(metadata);
            msg.setData("hello".getBytes());
            buffer = msg.pack();
            msg = new GrizzlyMessage(buffer);

            msg = new GrizzlyMessage();
            msg.setId(1);
            msg.setCommand("test");
            msg.setData("hello".getBytes());
            buffer = msg.pack();
            msg = new GrizzlyMessage(buffer);
        }
        System.out.println("cost: "+(System.currentTimeMillis()-st)+"ms");
    }

    @Test
    public void nettyMessage(){
        NettyMessage msg = new NettyMessage();
        msg.setId(1);
        msg.setCommand("test");
        MetaData metadata = new MetaData();
        metadata.add(2, "ddd");
        msg.setMetadata(metadata);
        msg.setData("hello".getBytes());
        ByteBuf buffer = msg.pack();
        log.info("buffer:{}", buffer);
        msg = new NettyMessage(buffer);
        log.info("msg:{}", msg);

        msg = new NettyMessage();
        msg.setId(1);
        msg.setCommand("test");
        msg.setData("hello".getBytes());
        buffer = msg.pack();
        log.info("buffer:{}", buffer);
        msg = new NettyMessage(buffer);
        log.info("msg:{}", msg);

        long st = System.currentTimeMillis();
        for(int i=0;i<TOTAL;i++){
            msg = new NettyMessage();
            msg.setId(1);
            msg.setCommand("test");
            metadata = new MetaData();
            metadata.add(2, "ddd");
            msg.setMetadata(metadata);
            msg.setData("hello".getBytes());
            buffer = msg.pack();
            msg = new NettyMessage(buffer);

            msg = new NettyMessage();
            msg.setId(1);
            msg.setCommand("test");
            msg.setData("hello".getBytes());
            buffer = msg.pack();
            msg = new NettyMessage(buffer);
        }
        System.out.println("cost: "+(System.currentTimeMillis()-st)+"ms");

    }
}
