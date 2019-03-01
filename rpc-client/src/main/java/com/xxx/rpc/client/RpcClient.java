package com.xxx.rpc.client;

import com.xxx.rpc.common.bean.RpcRequest;
import com.xxx.rpc.common.bean.RpcResponse;
import com.xxx.rpc.common.codec.RpcDecoder;
import com.xxx.rpc.common.codec.RpcEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RPC 客户端（用于发送 RPC 请求）
 */
public class RpcClient extends SimpleChannelInboundHandler<RpcResponse> {

  private static final Logger LOGGER = LoggerFactory.getLogger(RpcClient.class);

  private final String host;
  private final int port;

  private RpcResponse response;

  public RpcClient(String host, int port) {
    this.host = host;
    this.port = port;
  }

  @Override
  public void channelRead0(ChannelHandlerContext ctx, RpcResponse response) throws Exception {
    this.response = response;
  }

  public RpcResponse send(RpcRequest request) throws Exception {
    EventLoopGroup group = new NioEventLoopGroup();
    try {
      // 创建并初始化 Netty 客户端 Bootstrap 对象
      Bootstrap bootstrap = new Bootstrap();
      bootstrap.group(group);
      bootstrap.channel(NioSocketChannel.class);
      bootstrap.handler(new ChannelInitializer<SocketChannel>() {
        @Override
        protected void initChannel(SocketChannel channel) throws Exception {
          ChannelPipeline pipeline = channel.pipeline();
          pipeline.addLast(new RpcEncoder(RpcRequest.class));
          pipeline.addLast(new RpcDecoder(RpcResponse.class));
          pipeline.addLast(RpcClient.this);
        }
      });
      bootstrap.option(ChannelOption.TCP_NODELAY, true);
      // 连接 RPC 服务器
      ChannelFuture future = bootstrap.connect(host, port).sync();
      // 写入 RPC 请求数据并关闭连接
      Channel channel = future.channel();
      channel.writeAndFlush(request).sync();
      channel.closeFuture().sync();
      // 返回 RPC 响应对象
      return response;
    } finally {
      group.shutdownGracefully();
    }
  }
}
