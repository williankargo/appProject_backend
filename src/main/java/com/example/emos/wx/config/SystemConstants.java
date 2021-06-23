package com.example.emos.wx.config;


import lombok.Data;
import org.springframework.stereotype.Component;

// 封裝類，保存數據表中的常量值

@Data
@Component
public class SystemConstants {
    public String attendanceStartTime;
    public String attendanceTime;
    public String attendanceEndTime;
    public String closingStartTime;
    public String closingTime;
    public String closingEndTime;
}
