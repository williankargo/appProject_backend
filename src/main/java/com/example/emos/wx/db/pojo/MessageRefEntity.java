package com.example.emos.wx.db.pojo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;


// 紀錄讀了沒有
@Document(collection = "message_ref")
@Data
public class MessageRefEntity implements Serializable {

    @Id
    private String _id;

    @Indexed
    private String messageId; // 對應MessageEntity的_id

    @Indexed
    private Integer receiverId;

    @Indexed
    private Boolean readFlag;

    @Indexed
    private Boolean lastFlag;
}
