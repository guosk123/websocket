package com.machloop.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @author guosk
 *
 * create at 2024年03月11日, machloop
 */
@Component public class WebSSHServer {

  private static final Logger LOGGER = LoggerFactory.getLogger(WebSSHServer.class);

  private final String remoteHost = "127.0.0.1";
  private final int remotePort = 41115;
  private final int localPort = 41116;

  private Bootstrap bootstrap;

  @PostConstruct public void init() {
    WebSSHServer proxyServer = new WebSSHServer();
    proxyServer.start();
  }

  public void start() {
    NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
    NioEventLoopGroup workGroup = new NioEventLoopGroup(1);

    ServerBootstrap server = new ServerBootstrap();
    this.bootstrap = new Bootstrap();
    bootstrap.channel(NioSocketChannel.class);
    bootstrap.group(bossGroup);
    server.group(bossGroup, workGroup);
    server.channel(NioServerSocketChannel.class).handler(new LoggingHandler(LogLevel.INFO))
        .childHandler(new ChannelInitializer<SocketChannel>() {
          @Override protected void initChannel(SocketChannel ch) throws Exception {

            // 服务端channel，将服务端的数据发送给客户端，所以构造函数参数要传入客户端的channel
            ChannelPipeline pipeline = ch.pipeline();
            pipeline.addLast(new HttpObjectAggregator(1024 * 1024 * 1024));
            pipeline.addLast("serverHandler", new DataHandler(getClientChannel(ch)));
          }

          @Override public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            LOGGER.warn("client inactive, close connect");
            // 关闭流
            ctx.close();
          }

          @Override public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
              throws Exception {
            super.exceptionCaught(ctx, cause);
            ctx.close();
          }

          @Override public void userEventTriggered(ChannelHandlerContext ctx, Object evt)
              throws Exception {
            super.userEventTriggered(ctx, evt);
          }

          @Override public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
            super.channelUnregistered(ctx);
          }

        }).option(ChannelOption.SO_BACKLOG, 1024).option(ChannelOption.SO_SNDBUF, 16 * 1024)
        .option(ChannelOption.SO_RCVBUF, 16 * 1024).option(ChannelOption.SO_KEEPALIVE, true);

    server.bind(localPort).syncUninterruptibly()
        .addListener((ChannelFutureListener) channelFuture -> {
          if (channelFuture.isSuccess()) {
            LOGGER.info("webssh proxy server running, port: {}", localPort);
          } else {
            LOGGER.warn("webssh proxy server failed to start, port: {}", localPort);
          }
        });
  }

  private Channel getClientChannel(SocketChannel ch) throws InterruptedException {
    this.bootstrap.handler(new ChannelInitializer<SocketChannel>() {
      @Override protected void initChannel(SocketChannel socketChannel) {
        // 客户端端channel，客户端返回的数据给服务端，所以构造函数参数要传入服务端的channel
        socketChannel.pipeline().addLast("clientHandler", new DataHandler(ch));
      }

      @Override public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        LOGGER.warn("webssh server inactive, close connect");
        // 关闭流
        ctx.close();
      }

      @Override public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
          throws Exception {
        super.exceptionCaught(ctx, cause);
        ctx.close();
      }
    });
    ChannelFuture sync = bootstrap.connect(remoteHost, remotePort).sync();
    return sync.channel();
  }

  private static class DataHandler extends ChannelInboundHandlerAdapter {

    private final Channel channel;

    public DataHandler(Channel channel) {
      this.channel = channel;
    }

    @Override public void channelRead(ChannelHandlerContext ctx, Object msg) {
      ByteBuf byteBuf = (ByteBuf) msg;
      byteBuf.retain();
      channel.writeAndFlush(byteBuf);
      /*if (msg instanceof FullHttpRequest) {
        FullHttpRequest fullHttpRequest = (FullHttpRequest)msg;
        fullHttpRequest.retain();
        channel.writeAndFlush(fullHttpRequest);
      } else if (msg instanceof WebSocketFrame) {
        WebSocketFrame webSocketFrame = (WebSocketFrame) msg;
        webSocketFrame.retain();
        channel.writeAndFlush(webSocketFrame);
      }else {
        ByteBuf byteBuf = (ByteBuf) msg;
      byteBuf.retain();
      channel.writeAndFlush(byteBuf);
      }*/

    }

    @Override public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
      ctx.executor().schedule(() -> {
      }, 30, TimeUnit.SECONDS);
      super.channelRegistered(ctx);
    }

    @Override public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
      super.channelUnregistered(ctx);
      channel.close();

      ctx.writeAndFlush("Unregistered")
          .addListener((ChannelFutureListener) channelFuture -> channelFuture.channel().close());
    }

    @Override public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
      super.handlerRemoved(ctx);
      ctx.close();
      channel.close();
    }

    @Override public void channelInactive(ChannelHandlerContext ctx) throws Exception {
      super.channelInactive(ctx);
      ctx.close();
      channel.close();
    }
  }

  private static class ChannelAuthHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final Channel channel;

    public ChannelAuthHandler(Channel channel) {
      this.channel = channel;
    }

    @Override public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
      LOGGER.info("uri: {}, header: {}", request.uri(), request.headers().names());


      // 传递到下一个handler：升级握手
      ctx.fireChannelRead(request.retain());
      // 在本channel上移除这个handler消息处理，即只处理一次，鉴权通过与否
      ctx.pipeline().remove(ChannelAuthHandler.class);
      channel.writeAndFlush(request);
    }
  }

}
