package com.machloop.websocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * @author guosk
 *
 * create at 2024年03月06日, websocket
 */
@Configuration
@EnableWebSocket
public class WebSocketConfiguration  implements WebSocketConfigurer {

  @Autowired
  private WebSSH webSSH;

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(webSSH, "/webssh").setAllowedOrigins("*");
  }
}
