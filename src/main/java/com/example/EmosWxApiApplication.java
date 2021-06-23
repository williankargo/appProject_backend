package com.example;

import cn.hutool.core.util.StrUtil;
import com.example.emos.wx.config.SystemConstants;
import com.example.emos.wx.db.dao.SysConfigDao;
import com.example.emos.wx.db.pojo.SysConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

import javax.annotation.PostConstruct;
import java.lang.reflect.Field;
import java.util.List;

@SpringBootApplication
@ServletComponentScan // 讓filter生效
@Slf4j // 日誌
public class EmosWxApiApplication {

    @Autowired
    private SysConfigDao sysConfigDao;

    @Autowired
    private SystemConstants constants;


    public static void main(String[] args) {
        SpringApplication.run(EmosWxApiApplication.class, args);
    }


    @PostConstruct // 狹義點來看 constructor -> @AutoWired -> @PostConstruct 也就是說會在依賴注入完成後被“自動”調用
    public void init() {
        List<SysConfig> list = sysConfigDao.selectAllParam();
        list.forEach(one -> {
            String key = one.getParamKey();
            key = StrUtil.toCamelCase(key); // 因為從數據庫返回的key不是CamelCase
            String value = one.getParamValue();
            try {
                // 可以用constants.setXXX() 但是不知道要調哪個set，所以用反射大法
                Field field = constants.getClass().getDeclaredField(key); // 反射大法，直接把值灌入這個constants實例
                field.set(constants, value);
            } catch (Exception e) { // 反射時要處理異常
                log.error("執行異常", e);
            }
        });
    }


}
