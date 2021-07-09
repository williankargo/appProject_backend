package com.example.emos.wx.controller;


import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import com.example.emos.wx.common.util.R;
import com.example.emos.wx.config.SystemConstants;
import com.example.emos.wx.config.shiro.JwtUtil;
import com.example.emos.wx.controller.form.CheckinForm;
import com.example.emos.wx.controller.form.SearchMonthCheckinForm;
import com.example.emos.wx.exception.EmosException;
import com.example.emos.wx.service.CheckinService;
import com.example.emos.wx.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

@RequestMapping("/checkin")
@RestController // 可返回JSON格式
@Api(tags = "簽到模塊Web接口")
@Slf4j
public class CheckinController {

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${emos.image-folder}")
    private String imageFolder;

    @Autowired
    private CheckinService checkinService;

    @Autowired
    private UserService userService;

    @Autowired
    private SystemConstants constants;

    @GetMapping("/validCanCheckIn")
    @ApiOperation("查看用戶今天是否可以簽到")
    public R validCanCheckIn(@RequestHeader("token") String token) {  // 取得header名叫token的東西
        int userId = jwtUtil.getUserId(token);
        String result = checkinService.validCanCheckIn(userId, DateUtil.today());
        return R.ok(result);
    }


    // @RequestParam(接收來自於url或body，處理Content-Type: application/x-www-form-urlencoded)
    // @RequestBody(接收來自於body，處理 Content-Type: application/json)
    // todo: @沒有使用? 為什麼不用加上@RequestPart? 儘管前端傳來 Content-Type: application/form-data，
    // todo: photo出現在html的哪裏?
    @PostMapping("/checkin")
    @ApiOperation("簽到")//--------------------接收前端命名為photo的檔案
    public R checkin(@Valid CheckinForm form, @RequestParam("photo") MultipartFile file, @RequestHeader("token") String token) {

        if (file == null) {
            return R.error("沒有上傳文件");
        }

        int userId = jwtUtil.getUserId(token);
        String fileName = file.getOriginalFilename().toLowerCase();
        if (!fileName.endsWith(".jpg")) {
            return R.error("必須提交JPG格式的圖片");
        } else {
            String path = imageFolder + "/" + fileName;
            try {
                file.transferTo(Paths.get(path)); // file存到指定路徑
                HashMap param = new HashMap();
                param.put("userId", userId);
                param.put("path", path);
                param.put("city", form.getCity());
                param.put("district", form.getDistrict());
                param.put("address", form.getAddress());
                param.put("country", form.getCountry());
                param.put("province", form.getProvince());
                checkinService.checkin(param); // 這裡會調出path裡的東西進行驗證
                return R.ok("簽到成功");

            } catch (IOException e) {
                log.error(e.getMessage(), e);
                throw new EmosException("圖片保存錯誤");
            } finally {
                FileUtil.del(path);  // 簽到成功後就刪掉圖片
            }
        }
    }


    @PostMapping("/createFaceModel")
    @ApiOperation("創建人臉模型")
    public R createFaceModel(@RequestParam("photo") MultipartFile file, @RequestHeader("token") String token) {
        if (file == null) {
            return R.error("沒有上傳文件");
        }

        int userId = jwtUtil.getUserId(token);
        String fileName = file.getOriginalFilename().toLowerCase();
        if (!fileName.endsWith(".jpg")) {
            return R.error("必須提交JPG格式的圖片");
        } else {
            String path = imageFolder + "/" + fileName;
            try {
                file.transferTo(Paths.get(path)); // file存到指定路徑
                checkinService.createFaceModel(userId, path);
                return R.ok("人臉建模成功");

            } catch (IOException e) {
                log.error(e.getMessage(), e);
                throw new EmosException("圖片保存錯誤");
            } finally {
                FileUtil.del(path);  // 簽到成功後就刪掉圖片
            }
        }
    }


    @GetMapping("/searchTodayCheckin")
    @ApiOperation("查詢用戶當日簽到數據")
    public R searchTodayCheckin(@RequestHeader("token") String token) {

        int userId = jwtUtil.getUserId(token);
        HashMap map = checkinService.searchTodayCheckin(userId); // 到R對象的
        map.put("attendanceTime", constants.attendanceTime);
        map.put("closingTime", constants.closingTime);
        long days = checkinService.searchCheckinDays(userId);
        map.put("checkinDays", days);

        DateTime hiredate = DateUtil.parse(userService.searchUserHiredate(userId));
        DateTime startDate = DateUtil.beginOfWeek(DateUtil.date());
        if (startDate.isBefore(hiredate)) { // 防止把未到職的天數也算缺勤
            startDate = hiredate;
        }
        DateTime endDate = DateUtil.endOfWeek(DateUtil.date()); // 這裡已經設定endDate為一週後
        HashMap param = new HashMap(); // 到數據庫的
        param.put("startDate", startDate.toString());
        param.put("endDate", endDate.toString());
        param.put("userId", userId);
        ArrayList<HashMap> list = checkinService.searchWeekCheckin(param);

        map.put("weekCheckin", list);
        return R.ok().put("result", map);
    }

    @PostMapping("/searchMonthCheckin")
    @ApiOperation("查詢用戶某月簽到數據")
    public R searchMonthCheckin(@Valid @RequestBody SearchMonthCheckinForm form, @RequestHeader("token") String token) {

        int userId = jwtUtil.getUserId(token);
        DateTime hiredate = DateUtil.parse(userService.searchUserHiredate(userId));
        String month = form.getMonth() < 10 ? "0" + form.getMonth() : form.getMonth().toString(); // 小於10代表只有一位，要加個0變成兩位數
        DateTime startDate = DateUtil.parse(form.getYear() + "-" + month + "-01");

        // 要查詢的起始日期 比 入職日該月第一天的日期 還要早 (如果這個條件都過不了，那完全沒有查的機會)
        if (startDate.isBefore(DateUtil.beginOfMonth(hiredate))) {
            throw new EmosException("只能查詢考勤之後日期的數據");
        }
        // 要查詢的起始日期 比 入職日還早，把要查詢的起始日期放在入職日
        if (startDate.isBefore(hiredate)) {
            startDate = hiredate;
        }

        DateTime endDate = DateUtil.endOfMonth(startDate);
        HashMap param = new HashMap();
        param.put("userId", userId);
        param.put("startDate", startDate.toString());
        param.put("endDate", endDate.toString());
        ArrayList<HashMap> list = checkinService.searchMonthCheckin(param);

        int sum_1 = 0, sum_2 = 0, sum_3 = 0;
        for (HashMap<String, String> one : list) {
            String type = one.get("type");
            String status = one.get("status");
            if ("工作日".equals(type)) {
                if ("正常".equals(status)) {
                    sum_1++;
                } else if ("遲到".equals(status)) {
                    sum_2++;
                } else if ("缺勤".equals(status)) {
                    sum_3++;
                }
            }
        }

        return R.ok().put("list", list).put("sum_1", sum_1).put("sum_2", sum_2).put("sum_3", sum_3);
    }


}
