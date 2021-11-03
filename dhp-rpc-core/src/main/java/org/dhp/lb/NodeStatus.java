package org.dhp.lb;

import lombok.Data;

@Data
public class NodeStatus {
    String id;
    String path;
    String name;
    String host;
    //高可用状态 master(主), slave(从)
    String haValue;
    int port;
    //负荷率，计算公式为(memLoad+cpuLoad)/2
    Double totalLoad;
    //内存使用率
    Double memLoad;
    //CPU使用平均
    Double cpuLoad;
}
