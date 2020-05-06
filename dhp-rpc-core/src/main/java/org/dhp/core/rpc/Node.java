package org.dhp.core.rpc;

import lombok.Data;

@Data
public class Node {
    String name;
    String port;
    String host;
}
