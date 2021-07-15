package com.example.emos.wx.config;


import com.rabbitmq.client.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public ConnectionFactory getFactory(){
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("192.168.50.193"); // linux地址
        factory.setPort(5672);  // rabbitMQ端口號
        return factory;
    }


}
