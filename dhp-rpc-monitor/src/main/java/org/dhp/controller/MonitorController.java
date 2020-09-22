package org.dhp.controller;

import org.dhp.common.utils.JacksonUtil;
import org.dhp.core.rpc.RpcChannelPool;
import org.dhp.core.spring.DhpProperties;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RequestMapping("/dhp")
@RestController
public class MonitorController {

    @Resource
    DhpProperties dhpProperties;

    @Resource
    RpcChannelPool rpcChannelPool;

    @RequestMapping("/config")
    public String info() {
        return JacksonUtil.bean2Json(dhpProperties);
    }

    @RequestMapping("/channels")
    public String channels() {
        return JacksonUtil.bean2Json(rpcChannelPool.getAllChannels());
    }
}
