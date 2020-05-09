package org.dhp.core.rpc;

import lombok.Data;

@Data
public class Node {
    String name;
    int port;
    String host;
    long timeout;
}
