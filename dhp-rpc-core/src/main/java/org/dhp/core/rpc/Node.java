package org.dhp.core.rpc;

import lombok.Data;

/**
 * @author zhangcb
 */
@Data
public class Node implements Comparable<Node> {
    String id;
    /**
     * 同名Node可以允许多个存在
     */
    String name;
    int port;
    String host;
    String path;
    boolean enable = true;
    String haValue = "master";
    long timeout;
    int channelSize = 8;
    /**
     * 通过注册中心更新
     */
    double weight;

    @Override
    public int compareTo(Node o) {
        return this.weight > o.getWeight() ? 1 : -1;
    }

    @Override
    public int hashCode() {
        return name.hashCode()^host.hashCode()^port;
    }
}
