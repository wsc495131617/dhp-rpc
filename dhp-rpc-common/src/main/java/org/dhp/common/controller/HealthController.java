package org.dhp.common.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @RequestMapping("/dhp/health")
    public String health() {
        return "OK";
    }

    @RequestMapping("/dhp/time")
    public Long time() {
        return System.currentTimeMillis();
    }
}
