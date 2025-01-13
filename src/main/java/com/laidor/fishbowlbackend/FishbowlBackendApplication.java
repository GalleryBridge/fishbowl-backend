package com.laidor.fishbowlbackend;

import com.laidor.fishbowlbackend.socket.ServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@Slf4j
@SpringBootApplication
public class FishbowlBackendApplication implements CommandLineRunner {

    private final static int TCP_SERVER_PORT = 1883;
//    private final static int MQTT_SERVER_PORT = 1884;

    public static void main(String[] args) {
        SpringApplication.run(FishbowlBackendApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new LoggingHandler(LogLevel.DEBUG)); // 添加日志处理器
                            pipeline.addLast("mqttDecoder", new MqttDecoder());
                            pipeline.addLast("mqttEncoder", MqttEncoder.INSTANCE);
                            pipeline.addLast("customHandler", new ServerHandler());

                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            ChannelFuture tcpFuture = bootstrap.bind(TCP_SERVER_PORT).sync();
//            ChannelFuture mqttFuture = bootstrap.bind(MQTT_SERVER_PORT).sync();
            log.info("Netty server started on port " + TCP_SERVER_PORT);
            tcpFuture.channel().closeFuture().sync();
//            mqttFuture.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }


}
