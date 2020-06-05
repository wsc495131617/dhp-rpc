package org.dhp.core.spring;

import lombok.Data;
import org.dhp.core.rpc.ChannelType;
import org.dhp.core.rpc.Node;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * @author zhangcb
 */
@Data
@ConfigurationProperties(prefix = "dhp")
public class DhpProperties {
    static DhpProperties inst;
    
    /**
     * 注意：客户端和服务端同时启用的情况下 一定用这种方式使用，不能@Resource，具体解决办法，需要研究springboot的源码
     * @return
     */
    public static DhpProperties getInstance(){
        return inst;
    }
    
    public DhpProperties(){
        inst = this;
    }
    
    
    ChannelType type = ChannelType.Grizzly;
    int port;
    int workThread;
    int timeout = 15000;
    List<Node> nodes;
}
