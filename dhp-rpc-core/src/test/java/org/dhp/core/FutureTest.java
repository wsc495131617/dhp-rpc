package org.dhp.core;

import lombok.extern.slf4j.Slf4j;
import org.dhp.core.rpc.FutureImpl;

import java.util.concurrent.*;

@Slf4j
public class FutureTest {

    static ScheduledExecutorService service = Executors.newScheduledThreadPool(1);
    public static void main(String[] args) throws Throwable {
        final FutureImpl impl = new FutureImpl();
        service.schedule(()->{
            impl.result("done");
        }, 3, TimeUnit.SECONDS);
        Object ret = impl.get(1, TimeUnit.SECONDS);
        log.info("ret: {}", ret);
    }
}
