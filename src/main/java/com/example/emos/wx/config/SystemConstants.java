package com.example.emos.wx.config;


import lombok.Data;
import org.springframework.stereotype.Component;

// 因為這些常量信息和考勤模塊息息相關，所以放在config資料夾裡，項目啟動時緩存成java對象。且概念上全局都可以使用。

@Data
@Component // 納入spring容器，將來可以注入給其他java bean. 通俗的元件。
public class SystemConstants {
    public String attendanceStartTime;
    public String attendanceTime;
    public String attendanceEndTime;
    public String closingStartTime;
    public String closingTime;
    public String closingEndTime;
}
