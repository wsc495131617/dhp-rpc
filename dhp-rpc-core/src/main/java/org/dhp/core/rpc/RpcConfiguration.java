package org.dhp.core.rpc;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Data
@Slf4j
public class RpcConfiguration {

    RpcConfiguration(){
        log.info("初始化RpcConfiguration");
    }
    private String type = "grizzly";

    List<Node> nodes;

}
