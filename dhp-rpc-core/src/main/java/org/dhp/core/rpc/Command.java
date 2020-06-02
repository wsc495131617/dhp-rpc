package org.dhp.core.rpc;

import lombok.Data;

import java.lang.reflect.Method;

/**
 * @author zhangcb
 */
@Data
public class Command {
    Method method;
    String name;
    Class<?> cls;
    String nodeName;
}
