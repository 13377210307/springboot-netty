package com.netty.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

//@RestController
//@RequestMapping("/chart")
@Controller
public class ChartController {

    @RequestMapping("/")
    public String WebsocketChatClient(){
        return "/WebsocketChatClient";
    }


}
