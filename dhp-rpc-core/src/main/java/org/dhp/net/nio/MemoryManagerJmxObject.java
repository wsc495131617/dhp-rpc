package org.dhp.net.nio;

import lombok.extern.slf4j.Slf4j;
import org.glassfish.gmbal.GmbalMBean;
import org.glassfish.gmbal.ManagedObject;
import org.glassfish.grizzly.jmxbase.GrizzlyJmxManager;
import org.glassfish.grizzly.memory.MemoryManager;
import org.glassfish.grizzly.monitoring.jmx.JmxObject;

@Slf4j
@ManagedObject
public class MemoryManagerJmxObject extends JmxObject {

    MemoryProbe memoryProbe = new MemoryProbe();

    MemoryManager memoryManager;

    public MemoryManagerJmxObject(MemoryManager memoryManager) {
        this.memoryManager = memoryManager;
    }

    @Override
    public String getJmxName() {
        return "MemoryManagerJmxObject";
    }

    @Override
    protected void onRegister(GrizzlyJmxManager grizzlyJmxManager, GmbalMBean gmbalMBean) {
        memoryManager.getMonitoringConfig().addProbes(memoryProbe);
    }

    @Override
    protected void onDeregister(GrizzlyJmxManager grizzlyJmxManager) {

    }

    static class MemoryProbe implements org.glassfish.grizzly.memory.MemoryProbe {

        @Override
        public void onBufferAllocateEvent(int size) {
            log.debug("onBufferAllocateEvent: {}", size);
        }

        @Override
        public void onBufferAllocateFromPoolEvent(int size) {
//            log.info("onBufferAllocateFromPoolEvent: {}", size);
        }

        @Override
        public void onBufferReleaseToPoolEvent(int size) {
//            log.info("onBufferReleaseToPoolEvent: {}", size);
        }
    }

}
