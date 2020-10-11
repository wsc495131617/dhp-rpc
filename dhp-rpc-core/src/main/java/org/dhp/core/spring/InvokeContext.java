package org.dhp.core.spring;

import org.dhp.common.rpc.RpcHeader;

import java.util.HashMap;
import java.util.Map;

/**
 * 调用上下文，用于存放一些临时缓存
 */
public class InvokeContext {

    static ThreadLocal<Map<String, RpcHeader>> headerThreadLocal = ThreadLocal.withInitial(() -> {
        return new HashMap<>();
    });

    /**
     * 设置Header
     *
     * @param name
     * @param header
     */
    public static void setHeader(String name, RpcHeader header) {
        headerThreadLocal.get().put(name, header);
    }

    /**
     * 获取Header
     *
     * @param name
     * @return
     */
    public static RpcHeader getHeader(String name) {
        return headerThreadLocal.get().get(name);
    }

    /**
     * 清理Header
     * @param name
     */
    public static void clearHeader(String name) {
        headerThreadLocal.get().remove(name);
    }
}
