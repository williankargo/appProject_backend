package com.example.emos.wx.task;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
@Scope("prototype")
public class EmailTask implements Serializable{ // 定義線程池任務必須要implements Serializable

    @Autowired
    private JavaMailSender javaMailSender;

    @Value("${emos.email.system}")
    private String mailbox;

    @Async  // 實現多線程發送郵件
    public void sendAsync(SimpleMailMessage message){
        message.setFrom(mailbox);
        message.setCc(mailbox); // tip: 把郵件抄送給自己，避免被smtp認定為垃圾郵件
        javaMailSender.send(message);
    }

}