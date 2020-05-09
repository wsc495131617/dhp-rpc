package org.dhp.core;

import lombok.extern.slf4j.Slf4j;
import org.dhp.core.grizzly.GrizzlyMessage;
import org.dhp.core.rpc.MetaData;
import org.glassfish.grizzly.Buffer;
import org.junit.Test;

@Slf4j
public class MessageTests {

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

    }
}
