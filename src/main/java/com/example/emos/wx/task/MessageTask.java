package com.example.emos.wx.task;


import com.example.emos.wx.db.pojo.MessageEntity;
import com.example.emos.wx.exception.EmosException;
import com.example.emos.wx.service.MessageService;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.crypto.hash.Hash;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;
// java後端使用異步多線程，向RabbitMQ發起請求
// todo: 註：雖然是多線程，但MessageTask是通過函數參數傳入的，不存在多個線程使用同一個成員變量的情況，所以不用註解 @Scope("prototype")
@Component
@Slf4j
public class MessageTask {

    @Autowired
    private ConnectionFactory factory;

    @Autowired
    private MessageService messageService;

    // 同步發送消息
    public void send(String topic, MessageEntity entity) { // 在RabbitMQ裡面每個消息都有自己的topic
        String id = messageService.insertMessage(entity); // 向mongodb保存消息數據，返回消息ID

        // 向RabbitMQ發送消息
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel(); // 執行完後connection/ channel會自動關閉
        ) {
            // 連接到某個topic
            channel.queueDeclare(topic, true, false, false, null); // true->存在硬盤, false->沒有鎖，不排他, false-> 使用完後不會自動刪除
            HashMap map = new HashMap();
            map.put("messageId", id); // 存放屬性數據

            // 創建AMQP協議參數對象，添加附加屬性
            AMQP.BasicProperties properties = new AMQP.BasicProperties().builder().headers(map).build();
            channel.basicPublish("", topic, properties, entity.getMsg().getBytes()); // ""->路由地址
            log.debug("消息發送成功");
        } catch (Exception e) {
            log.error("執行異常", e);
            throw new EmosException("向MQ發送消息失敗");
        }
    }

    // 異步發送消息
    @Async // 異步執行
    public void sendAsync(String topic, MessageEntity entity) {
        send(topic, entity);
    }
}

