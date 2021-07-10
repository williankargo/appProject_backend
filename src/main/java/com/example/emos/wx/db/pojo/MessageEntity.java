package com.example.emos.wx.db.pojo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.Date;

// 紀錄發送的消息
@Data
@Document(collection = "message") // 對應mongoDB中的collection
public class MessageEntity implements Serializable {

    @Id // 主鍵
    private String _id;

    @Indexed(unique = true) // 唯一性索引，防止消息被重複消費
    private String uuid;

    @Indexed // 建立索引方便mongoDB對欄位進行搜索
    private Integer senderId;

    private String senderPhoto="https://enos-resource-1306307505.cos.ap-hongkong.myqcloud.com/img/System.JPG";

    private String senderName;

    private String msg;

    @Indexed
    private Date sendTime;

}
