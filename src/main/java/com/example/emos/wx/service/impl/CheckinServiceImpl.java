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
import com.example.emos.wx.db.pojo.TbCheckin;
import com.example.emos.wx.db.pojo.TbCity;
import com.example.emos.wx.db.pojo.TbHolidays;
import com.example.emos.wx.db.pojo.TbWorkday;
import com.example.emos.wx.exception.EmosException;
import com.example.emos.wx.service.CheckinService;
import com.example.emos.wx.service.SmallTools.ChineseConverter;
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

    @Autowired  // 將Spring託管的bean注入我們的應用程序
    private SystemConstants constants;

    @Autowired
    private TbHolidaysDao holidaysDao; // dao

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

                /**
                 * 查詢疫情風險等級
                 * */
                int risk = 1;
                String city = (String) param.get("city");
                String district = (String) param.get("district");
                String country = (String) param.get("country"); // todo: 看到底會不會拿nation(會寫成country要改成nation)
                String province = (String) param.get("province");

                if ("中国".equals(country) && !StrUtil.isBlank(city) && !StrUtil.isBlank(district)) { // 只有中國和中國台灣能查到資料
                    try {
                        // 台灣的方法
                        if ("台湾省".equals(province)) {
                            // 全部縣市
                            String url2 = "https://covid-19.nchc.org.tw/city_confirmed.php?mycity=全部縣市";
                            Document document2 = Jsoup.connect(url2).get();
                            Elements elements2 = document2.getElementsByClass("country_deaths mb-1 text-dark display-4");

                            Element element2 = elements2.get(0);
                            String result2 = element2.select("h1").text(); //todo: +有影響嗎?
                            int sum = Integer.parseInt(result2);

                            // 單一縣市
                            city = ChineseConverter.CHconverter(city);
                            String url = "https://covid-19.nchc.org.tw/city_confirmed.php?mycity=" + city; // 精度只能到city
                            Document document = Jsoup.connect(url).get();
                            Elements elements = document.getElementsByClass("country_deaths mb-1 text-dark display-4");

                            Element element = elements.get(0);
                            String result = element.select("h1").text(); //todo: +有影響嗎?
                            int num = Integer.parseInt(result);

                            if (num / sum > 1 / 3) { // 高風險
                                risk = 3;
                                // todo: 發送警告郵件
                            } else if (num / sum < 1 / 3) { // 低風險
                                risk = 2;
                            }
                            // 中國的方法
                        } else {

                            String code = cityDao.searchCode(city);

                            String url = "http://m." + code + ".bendibao.com/news/yqdengji/?qu=" + district;
                            Document document = Jsoup.connect(url).get();
                            Elements elements = document.getElementsByClass("list-content");
                            if (elements.size() > 0) {
                                Element element = elements.get(0); // 取得第一個出現的list-content
                                String result = element.select("p:last-child").text(); // 取得最後一個p
                                if ("高风险".equals(result)) {
                                    risk = 3;
                                    // todo:發送警告郵件
                                } else if ("中风险".equals(result)) {
                                    risk = 2;
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.error("執行異常", e);
                        throw new EmosException("獲取風險等級失敗");
                    }
                }

                /**
                 * 保存簽到紀錄
                 * */
                String address = (String) param.get("address");

                TbCheckin entity = new TbCheckin(); // PO對象，為什麼不用Autowired?
                entity.setUserId(userId);
                entity.setAddress(address);
                entity.setCountry(country);
                entity.setProvince(province);
                entity.setCity(city);
                entity.setDistrict(district);
                entity.setStatus((byte) status);
                entity.setDate(DateUtil.today()); // DateUtil.today()返回String日期
                entity.setCreateTime(d1);
                checkinDao.insert(entity);

            }
        }
    }
}

