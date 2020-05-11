package com.example.webdemo.controller;

import org.dhp.examples.rpcdemo.pojo.HelloRequest;
import org.dhp.examples.rpcdemo.pojo.HelloResponse;
import org.dhp.examples.rpcdemo.service.IHelloService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RequestMapping("/hello")
@RestController
public class HelloController {

    @Resource
    IHelloService service;

    @RequestMapping("/say")
    public String say() {
        HelloRequest request = new HelloRequest();
        HelloResponse response = service.say(request);
        return response.getContent();
    }
}
