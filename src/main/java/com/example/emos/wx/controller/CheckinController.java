package com.example.emos.wx.controller;


import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import com.example.emos.wx.common.util.R;
import com.example.emos.wx.config.shiro.JwtUtil;
import com.example.emos.wx.controller.form.CheckinForm;
import com.example.emos.wx.exception.EmosException;
import com.example.emos.wx.service.CheckinService;
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
import java.util.HashMap;
import java.util.Locale;

@RequestMapping("/checkin")
@RestController
@Api(tags = "簽到模塊Web接口")
@Slf4j
public class CheckinController {

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${emos.image-folder}")
    private String imageFolder;

    @Autowired
    private CheckinService checkinService;

    @GetMapping("/validCanCheckIn")
    @ApiOperation("查看用戶今天是否可以簽到")
    public R validCanCheckIn(@RequestHeader("token") String token) {  // 取得header名叫token的東西
        int userId = jwtUtil.getUserId(token);
        String result = checkinService.validCanCheckIn(userId, DateUtil.today());
        return R.ok(result);
    }


    @PostMapping("/checkin")
    @ApiOperation("簽到")//--------------------接收前端命名為photo的檔案
    public R checkin(@Valid CheckinForm form, @RequestParam("photo") MultipartFile file, @RequestHeader("token") String token) {

        if (file == null) {
            return R.error("沒有上傳文件");
        }

        int userId = jwtUtil.getUserId(token);
        String fileName = file.getOriginalFilename().toLowerCase();
        if (!fileName.endsWith(".jpg")) { // todo: iphone可能不是jpg
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
                checkinService.checkin(param);
                return R.ok("簽到成功");

            } catch (IOException e) {
                log.error(e.getMessage(), e);
                throw new EmosException("圖片保存錯誤");
            } finally {
                FileUtil.del(path);  // 簽到成功後就刪掉圖片 todo: 幹阿沒有驗證啊？
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
        if (!fileName.endsWith(".jpg")) { // todo: iphone可能不是jpg
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

}
