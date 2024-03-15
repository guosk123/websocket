package com.machloop.websocket;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author guosk
 *
 * create at 2024年03月06日, websocket
 */
@RestController
@RequestMapping("/restapi")
public class Controller {

  @GetMapping("hello")
  public String hello(String name){
    return "hello " + name;
  }
}
