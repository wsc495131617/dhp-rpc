package org.dhp.core.grizzly;

import org.glassfish.grizzly.memory.HeapMemoryManager;
import org.glassfish.grizzly.memory.MemoryManager;

public class GrizzlyGlobal {
    public static MemoryManager memoryManager = new HeapMemoryManager(1024*1024*32);
}
