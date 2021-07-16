package com.example.emos.wx.task;


import com.example.emos.wx.db.pojo.MessageEntity;
import com.example.emos.wx.db.pojo.MessageRefEntity;
import com.example.emos.wx.exception.EmosException;
import com.example.emos.wx.service.MessageService;
import com.rabbitmq.client.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.crypto.hash.Hash;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

// java後端使用異步多線程，向RabbitMQ發起請求
// 註：不存在多個線程使用同一個task的情況，這裡每個線程會負責一個task中途也不會轉交給其他線程執行，所以不用註解 @Scope("prototype")
@Component
@Slf4j
public class MessageTask {

    @Autowired
    private ConnectionFactory factory;

    @Autowired
    private MessageService messageService;

    // 同步發送消息：後端向MQ存入消息，並先在Message collection(紀錄消息)保存消息
    public void send(String topic, MessageEntity entity) { // 在RabbitMQ裡面每個消息都有自己的topic
        String id = messageService.insertMessage(entity); // 向mongodb(MessageDao)保存消息數據，返回消息ID

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

    // 同步接收消息：後端接收MQ的消息，並存到DB(MessageRef collection(紀錄接收人和已讀狀態))
    public int receive(String topic) {
        int num = 0;
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel(); // 執行完後connection/ channel會自動關閉
        ) {
            channel.queueDeclare(topic, true, false, false, null); // 連接到某個topic
            while (true) {
                GetResponse response = channel.basicGet(topic, false); // 等正確寫入到ref再返回ack應答
                if (response != null) {
                    AMQP.BasicProperties properties = response.getProps();
                    Map<String, Object> map = properties.getHeaders();
                    String messageId = map.get("messageId").toString(); // 消息ID
                    byte[] body = response.getBody(); // 消息正文
                    String message = new String(body);
                    log.debug("從rabbitMQ接收的消息：" + message);

                    MessageRefEntity entity = new MessageRefEntity();
                    entity.setMessageId(messageId);
                    entity.setReceiverId(Integer.parseInt(topic));
                    entity.setReadFlag(false); // 未讀
                    entity.setLastFlag(true); // 最新的數據
                    messageService.insertRef(entity);

                    // 對rabbitMQ返回ACK應答，rabbitMQ並刪除數據
                    long deliveryTag = response.getEnvelope().getDeliveryTag();
                    channel.basicAck(deliveryTag, false);
                    num++; // 成功處理了一條消息
                } else {
                    break; // 已接收完所有消息
                }
            }

        } catch (Exception e) {
            log.error("執行異常", e);
            throw new EmosException("接收消息失敗");
        }
        return num;
    }

    // 異步接收消息
    @Async // 異步執行
    public void receiveAsync(String topic) {
        receive(topic);
    }

    // 刪除消息隊列(rabbitMQ)
    public void deleteQueue(String topic) {
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel(); // 執行完後connection/ channel會自動關閉
        ) {
            channel.queueDelete(topic);
            log.debug("rabbitMQ隊列成功刪除");
        } catch (Exception e) {
            log.error("刪除rabbitMQ隊列失敗", e);
            throw new EmosException("刪除rabbitMQ隊列失敗");
        }
    }

    @Async
    public void deleteQueueAsync(String topic){
        deleteQueue(topic);
    }
}
