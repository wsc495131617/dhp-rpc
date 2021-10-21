package org.dhp.core.spring;

import lombok.Data;
import org.dhp.core.rpc.ChannelType;
import org.dhp.core.rpc.Node;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Vector;

/**
 * @author zhangcb
 */
@Data
@ConfigurationProperties(prefix = "dhp")
public class DhpProperties {
    ChannelType type = ChannelType.Grizzly;
    int port;
    String name;
    int workThread;
    int timeout = 15000;
    Vector<Node> nodes;
}
