package org.dhp.lb;

import lombok.Data;

@Data
public class NodeStatus {
    String path;
    String name;
    String host;
    int port;
    //负荷率，计算公式为(memLoad+cpuLoad)/2
    Double totalLoad;
    //内存使用率
    Double memLoad;
    //CPU使用平均
    Double cpuLoad;
}
