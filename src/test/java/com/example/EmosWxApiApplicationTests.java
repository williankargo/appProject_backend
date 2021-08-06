package com.example;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.example.emos.wx.db.pojo.MessageEntity;
import com.example.emos.wx.db.pojo.MessageRefEntity;
import com.example.emos.wx.db.pojo.TbMeeting;
import com.example.emos.wx.service.MeetingService;
import com.example.emos.wx.service.MessageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;

@SpringBootTest
class EmosWxApiApplicationTests {

    @Autowired
    private MessageService messageService;

    @Autowired
    private MeetingService meetingService;

    @Test
    void contextLoads() { // 插入mongoDB測試數據
        for (int i = 1; i <= 100; i++) {
            MessageEntity message = new MessageEntity();
            message.setUuid(IdUtil.simpleUUID());
            message.setSenderId(0);
            message.setSenderName("系統消息");
            message.setMsg("這是第" + i + "條測試消息");
            message.setSendTime(new Date());
            String id = messageService.insertMessage(message);

            MessageRefEntity ref = new MessageRefEntity();
            ref.setMessageId(id);
            ref.setReceiverId(16); // 接收人ID (接收人目前的ID)
            ref.setLastFlag(true);
            ref.setReadFlag(false);
            messageService.insertRef(ref);

        }
    }

    @Test
    void createMeetingData() {
        for (int i = 1; i <= 100; i++) {
            TbMeeting meeting = new TbMeeting();
            meeting.setId((long) i);
            meeting.setUuid(IdUtil.simpleUUID());
            meeting.setTitle("測試會議" + i);
            meeting.setCreatorId(16L); //ROOT用戶ID
            meeting.setDate(DateUtil.today());
            meeting.setPlace("線上會議室");
            meeting.setStart("08:30");
            meeting.setEnd("10:30");
            meeting.setType((short) 1);
            meeting.setMembers("[15,16]");
            meeting.setDesc("會議研討Emos項目上線測試");
            meeting.setInstanceId(IdUtil.simpleUUID());
            meeting.setStatus((short) 3);
            meetingService.insertMeeting(meeting);

        }
    }
}

