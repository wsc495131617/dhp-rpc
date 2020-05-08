package org.dhp.core.spring;

import lombok.Data;
import org.dhp.core.rpc.Node;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Data
@ConfigurationProperties(prefix = "dhp")
public class DhpProperties {
    String type = "grizzly"; // grizzly netty
    int port;
    List<Node> nodes;
}
