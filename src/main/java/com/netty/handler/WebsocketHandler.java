package com.netty.handler;

import com.netty.utils.RandomName;
import com.netty.utils.RedisUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.rmi.activation.ActivationSystem;

@Component
@Qualifier("websocketHandler")
@ChannelHandler.Sharable
public class WebsocketHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    public static ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    @Autowired
    private RedisUtil redisUtil;

    // 活跃用户
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        System.out.println("用户 "+redisUtil.get(channel.id().toString())+"在线");
    }

    // 非活跃用户
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        System.out.println("用户 "+redisUtil.get(channel.id().toString())+"掉线");
    }

    // 连接成功
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        System.out.println(ctx.channel().remoteAddress());
        String username = new RandomName().getRandomName();

        Channel inComing = ctx.channel();
        //遍历所有通道
        for (Channel channel : channels) {
            // 将信息写入到通道中
            channel.writeAndFlush(new TextWebSocketFrame("[新用户："+username+"]"+"加入群聊"));
        }
        // 将通道id作为key，用户名作为value保存在redis
        this.redisUtil.set(inComing.id().toString(),username);
        // 将该通道放到通道集合中
        channels.add(ctx.channel());
    }

    // 断开
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        // 根据通道id从redis中获取用户名
        String username  = (String)this.redisUtil.get(ctx.channel().id().toString());
        for (Channel channel: channels) {
            channel.writeAndFlush(new TextWebSocketFrame("[用户："+username+"]"+"退出群聊"));
        }
        // 删除redis中保存的通道信息
        this.redisUtil.del(String.valueOf(ctx.channel().id()));
        // 将该通道放到通道集合中
        channels.add(ctx.channel());
    }

    // 读取数据并进行转发
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame text) throws Exception {
        // 获取通道
        Channel inComing = ctx.channel();
        // 获取用户名称
        String username = (String)this.redisUtil.get(String.valueOf(inComing.id()));
        // 遍历所有通道
        for (Channel channel : channels) {
            // 判断本身通道与其他通道
            if (channel != inComing) {
                // 其他通道
                channel.writeAndFlush(new TextWebSocketFrame(username+"："+text.text()));
            }else {
                // 本身通道
                channel.writeAndFlush(new TextWebSocketFrame("自己："+text.text()));
            }
        }
    }

    // 异常处理
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Channel channel = ctx.channel();
        // 根据用户id在redis中查询
        System.out.println("用户 "+redisUtil.get(channel.id().toString())+"出现异常，已关闭连接");
        cause.printStackTrace();
        ctx.close();
    }
}
