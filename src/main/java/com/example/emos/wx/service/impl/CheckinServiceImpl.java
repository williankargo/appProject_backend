package com.example.emos.wx.service.impl;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.example.emos.wx.config.SystemConstants;
import com.example.emos.wx.db.dao.*;
import com.example.emos.wx.db.pojo.TbCity;
import com.example.emos.wx.db.pojo.TbHolidays;
import com.example.emos.wx.db.pojo.TbWorkday;
import com.example.emos.wx.exception.EmosException;
import com.example.emos.wx.service.CheckinService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;


@Service
@Scope("prototype") // todo: 為了之後的郵件異步傳輸
@Slf4j
public class CheckinServiceImpl implements CheckinService {

    @Autowired
    private SystemConstants constants;

    @Autowired
    private TbHolidaysDao holidaysDao;

    @Autowired
    private TbWorkdayDao workdayDao;

    @Autowired
    private TbCheckinDao checkinDao;

    @Autowired
    private TbFaceModelDao faceModelDao;

    @Autowired
    private TbCityDao cityDao;

    @Value("${emos.face.createFaceModelUrl}")
    private String createFaceModelUrl;

    @Value("${emos.face.checkinUrl}")
    private String checkinUrl;

    @Override
    public String validCanCheckIn(int userId, String date) {
        boolean bool_1 = holidaysDao.searchTodayIsHolidays() != null ? true : false;
        boolean bool_2 = workdayDao.searchTodayIsWorkday() != null ? true : false;

        String type = "工作日";
        if (DateUtil.date().isWeekend()) {
            type = "節假日";
        }
        if (bool_1) {
            type = "節假日";
        } else if (bool_2) {
            type = "工作日";
        }

        if (type.equals("節假日")) {
            return "節假日不需要打卡";
        } else {
            DateTime now = DateUtil.date();
            String start = DateUtil.today() + " " + constants.attendanceStartTime;
            String end = DateUtil.today() + " " + constants.attendanceEndTime;
            DateTime attendanceStart = DateUtil.parse(start);
            DateTime attendanceEnd = DateUtil.parse(end);
            if (now.isBefore(attendanceStart)) {
                return "沒到上班打卡開始時間";
            } else if (now.isAfter(attendanceEnd)) {
                return "超過了上班打卡結束時間";
            } else {
                HashMap map = new HashMap();
                map.put("userId", userId);
                map.put("date", date);
                map.put("start", start);
                map.put("end", end);
                boolean bool = checkinDao.haveCheckin(map) != null ? true : false;
                return bool ? "今日已經打卡，不用重複打卡" : "可以打卡";
            }
        }
    }

    @Override
    public void checkin(HashMap param) { // todo: param具體是什麼我還要確認

        Date d1 = DateUtil.date(); // 當前時間
        Date d2 = DateUtil.parse(DateUtil.today() + " " + constants.attendanceTime); // 上班時間
        Date d3 = DateUtil.parse(DateUtil.today() + " " + constants.attendanceEndTime); // 簽到結束時間

        int status = 1;
        if (d1.compareTo(d2) <= 0) {
            status = 1; // 正常簽到時間
        } else if (d1.compareTo(d2) > 0 && d1.compareTo(d3) < 0) {
            status = 2; // 遲到
        }

        int userId = (Integer) param.get("userId");
        String faceModel = faceModelDao.searchFaceModel(userId);
        if (faceModel == null) {
            throw new EmosException("不存在人臉模型");
        } else {
            String path = (String) param.get("path");
            HttpRequest request = HttpUtil.createPost(checkinUrl);
            // 上傳圖片文件，後面是python提交參數的名字和數據庫中存有的faceModel
            request.form("photo", FileUtil.file(path), "targetModel", faceModel);
            HttpResponse response = request.execute();
            if (response.getStatus() != 200) {
                log.error("人臉識別服務異常"); // 輸出Slf4j日誌
                throw new EmosException("人臉識別服務異常");
            }
            String body = response.body();
            if ("无法识别出人脸".equals(body) || "照片中存在多张人脸".equals(body)) {
                throw new EmosException(body);
            } else if ("False".equals(body)) {
                throw new EmosException("簽到無效，非本人簽到");
            } else if ("True".equals(body)) {

                int risk = 1;
                String city = (String) param.get("city");
                String district = (String) param.get("district");

                // 查詢疫情風險等級，TW不適用
                if (!StrUtil.isBlank(city) && !StrUtil.isBlank(district)) {
                    String code = cityDao.searchCode(city);
                    try {
                        String url = "http://m." + code + ".bendibao.com/news/yqdengji/?qu=" + district;
                        Document document = Jsoup.connect(url).get();
                        Elements elements = document.getElementsByClass("list-content");
                        if (elements.size() > 0) {
                            Element element = elements.get(0); // 取得第一個
                            String result = element.select("p:last-child").text();
                            if ("高风险".equals(result)) {
                                risk = 3;
                                // todo:發送警告郵件
                            } else if ("低风险".equals(result)) {
                                risk = 2;
                            }
                        }
                    } catch (Exception e) {
                        log.error("執行異常", e);
                    }
                }
            }
        }

    }
}
