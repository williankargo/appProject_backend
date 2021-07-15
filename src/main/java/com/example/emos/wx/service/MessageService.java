package com.example.emos.wx.service;

import com.example.emos.wx.db.pojo.MessageEntity;
import com.example.emos.wx.db.pojo.MessageRefEntity;

import java.util.HashMap;
import java.util.List;

public interface MessageService {

    // from MessageDao
    public String insertMessage(MessageEntity entity); // shift + rename可以快速改名
    public List<HashMap> searchMessageByPage(int userId, long start, int length);
    public HashMap searchMessageById(String id);

    // from MessageRefDao
    public String insertRef(MessageRefEntity entity);
    public long searchUnreadCount(int userId);
    public long searchLastCount(int userId);
    public long updateUnreadMessage(String id);
    public long deleteMessageRefById(String id);
    public long deleteUserMessageRef(int userId);

}