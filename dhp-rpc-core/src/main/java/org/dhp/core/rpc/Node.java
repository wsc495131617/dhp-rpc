package org.dhp.core.rpc;

import lombok.Data;

/**
 * @author zhangcb
 */
@Data
public class Node implements Comparable<Node> {
    /**
     * 同名Node可以允许多个存在
     */
    String name;
    int port;
    String host;
    String path;
    long timeout;
    int channelSize = 4;
    /**
     * 通过注册中心更新
     */
    double weight;

    @Override
    public int compareTo(Node o) {
        return this.weight > o.getWeight() ? 1 : -1;
    }
}
