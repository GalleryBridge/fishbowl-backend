package com.laidor.fishbowlbackend.socket;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * @Author: Laidor
 * @Description: Netty服务类
 * @Date:2025/1/13 下午 01:15
 */
public class ServerHandler extends SimpleChannelInboundHandler<MqttMessage> {

    private final static Logger log = LoggerFactory.getLogger(ServerHandler.class);

    public final static String MSG_TOPIC = "device_msg_topic";

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MqttMessage msg) throws Exception {
        //  获取消息类型
        //  log.info("Received MQTT message of type: {}", msg.fixedHeader().messageType());
        MqttMessageType messageType = msg.fixedHeader().messageType();

        if (messageType == MqttMessageType.CONNECT) {

            // 构造 CONNACK 消息
            MqttFixedHeader connAckHeader = new MqttFixedHeader(
                    MqttMessageType.CONNACK,
                    false,
                    MqttQoS.AT_MOST_ONCE,
                    false,
                    0
            );
            MqttConnAckVariableHeader connAckVariableHeader = new MqttConnAckVariableHeader(
                    MqttConnectReturnCode.CONNECTION_ACCEPTED,
                    false
            );
            MqttConnAckMessage connAckMessage = new MqttConnAckMessage(connAckHeader, connAckVariableHeader);

            // 发送 CONNACK 给客户端
            ctx.writeAndFlush(connAckMessage);
            log.info("Sent CONNACK to client: {}", ctx.channel().remoteAddress());
        } else if (messageType == MqttMessageType.PUBLISH) {
            MqttPublishMessage publishMessage = (MqttPublishMessage) msg;

            // 获取并处理消息负载
            ByteBuf payload = publishMessage.payload();
            String messageContent = payload.toString(StandardCharsets.UTF_8);

            //  打印消息
            log.info("Received PUBLISH message with content: {}", messageContent);
        } else {
            log.warn("Received unsupported MQTT message type: {}", messageType);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("Client connected: {}", ctx.channel().remoteAddress());
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("Client disconnected: {}", ctx.channel().remoteAddress());
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof java.net.SocketException && "Connection reset".equals(cause.getMessage())) {
            System.out.println("Client connection reset: " + ctx.channel().remoteAddress());
        } else {
            cause.printStackTrace();
        }
        ctx.close(); // 关闭连接
    }

}
