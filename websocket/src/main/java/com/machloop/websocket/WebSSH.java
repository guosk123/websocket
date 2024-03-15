package com.machloop.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

/**
 * @author guosk
 *
 * create at 2024年03月05日, machloop
 */
@Component
public class WebSSH extends AbstractWebSocketHandler implements WebSocketHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(WebSSH.class);

  @Override
  public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    LOGGER.info("receive a webSSH connection, session id: {}", session.getId());
    // TODO 建立与webssh的连接

  }

  @Override
  public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    LOGGER.info("client msg: {}", message.getPayload());
    session.sendMessage(new TextMessage("hello " + message.getPayload()));
  }

  @Override
  public void handlePongMessage(WebSocketSession session, PongMessage message) throws Exception {
    session.sendMessage(new TextMessage("Pong!!!!!"));
  }

  @Override
  public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
    LOGGER.info("an error occurred with the webssh connection, session id: {}", session.getId(),
        exception);
    // TODO 查看当前是否与webssh建立了连接，如果是则主动断开连接
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus)
      throws Exception {
    LOGGER.info("close a webSSH connection, session id: {}, close: [code： {}， reason: {}]",
        session.getId(), closeStatus.getCode(), closeStatus.getReason());
    // TODO 客户端与本机断开连接，本机断开与webssh的连接
  }

}
