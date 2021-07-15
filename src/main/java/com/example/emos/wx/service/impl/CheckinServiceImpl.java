package com.example.emos.wx.service.impl;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateRange;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.example.emos.wx.config.SystemConstants;
import com.example.emos.wx.db.dao.*;
import com.example.emos.wx.db.pojo.*;
import com.example.emos.wx.exception.EmosException;
import com.example.emos.wx.service.CheckinService;
import com.example.emos.wx.service.SmallTools.ChineseConverter;
import com.example.emos.wx.task.EmailTask;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;


@Service
@Scope("prototype") // 為了之後的郵件異步傳輸
@Slf4j
public class CheckinServiceImpl implements CheckinService {

    // 將Spring託管的bean注入我們的應用程序。創建的時候需要對變量做初始化和注入依賴的java類，才需要加上@Autowired。
    // 透過這樣的機制，不需要再去new一個DAO
    // 元件在後端運行時只會存在一個，任何地方所使用的某個元件都是指向同一個。我有一條狗妹妹有一條狗，我們的狗是同一條狗，都存在這棟房子中。
    @Autowired
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

    @Autowired
    private TbUserDao userDao;

    @Value("${emos.face.createFaceModelUrl}")
    private String createFaceModelUrl;

    @Value("${emos.face.checkinUrl}")
    private String checkinUrl;

    @Value("${emos.email.hr}")
    private String hrEmail;

    @Autowired
    private EmailTask emailTask;

    @Value("${emos.code}")
    private String code;


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
            return "節假日不需要考勤";
        } else {
            DateTime now = DateUtil.date();
            String start = DateUtil.today() + " " + constants.attendanceStartTime;
            String end = DateUtil.today() + " " + constants.attendanceEndTime;
            DateTime attendanceStart = DateUtil.parse(start);
            DateTime attendanceEnd = DateUtil.parse(end);
            if (now.isBefore(attendanceStart)) {
                return "沒到上班考勤開始時間";
            } else if (now.isAfter(attendanceEnd)) {
                return "超過了上班考勤結束時間";
            } else {
                HashMap map = new HashMap();
                map.put("userId", userId);
                map.put("date", date);
                map.put("start", start);
                map.put("end", end);
                boolean bool = checkinDao.haveCheckin(map) != null ? true : false;
                return bool ? "今日已經考勤，不用重複考勤" : "可以考勤";
            }
        }
    }

    @Override
    public void checkin(HashMap param) {

        // VO/BO不能互傳通用
        // VO: 可能有很多驗證的註解，只有controller能用
        // BO: Service在用的，這裡在controller就把PO轉成BO對象HashMap了

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
            request.form("code", code); // 資源提取碼
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
                int risk = 1; // 默認低風險
                String city = (String) param.get("city");
                String district = (String) param.get("district");
                String country = (String) param.get("country");
                String province = (String) param.get("province");
                String address = (String) param.get("address");

                if ("中国".equals(country) && !StrUtil.isBlank(city) && !StrUtil.isBlank(district)) { // 只有中國和中國台灣能查到資料
                    try {
                        // 台灣的方法
                        if ("台湾省".equals(province)) {
                            // 全部縣市
                            String url2 = "https://covid-19.nchc.org.tw/city_confirmed.php?mycity=" + "全部縣市";
                            Document document2 = Jsoup.connect(url2).get(); // https需要去找憑證插進去
                            Elements elements2 = document2.getElementsByClass("country_deaths mb-1 text-dark display-4");

                            Element element2 = elements2.get(0);
                            String result2 = element2.select("h1").text(); //+沒有影響嗎
                            int sum = Integer.parseInt(result2);

                            // 單一縣市
                            city = ChineseConverter.CHconverter(city);
                            String url = "https://covid-19.nchc.org.tw/city_confirmed.php?mycity=" + city; // 精度只能到city
                            Document document = Jsoup.connect(url).get();
                            Elements elements = document.getElementsByClass("country_deaths mb-1 text-dark display-4");

                            Element element = elements.get(0);
                            String result = element.select("h1").text();
                            int num = Integer.parseInt(result);

                            double judge = (double) num / sum;
                            if (judge > 1 / 3.0) { // 高風險
                                risk = 3;
                                // 發送警告郵件
                                HashMap<String, String> map = userDao.searchNameAndDept(userId);
                                String name = map.get("name");
                                String deptName = map.get("dept_name");
                                deptName = deptName != null ? deptName : "";
                                SimpleMailMessage message = new SimpleMailMessage();
                                message.setTo(hrEmail);
                                message.setSubject("員工" + name + "身處高風險疫情第區警告");
                                message.setText(deptName + "員工" + name + "，" + DateUtil.format(new Date(),
                                        "yyyy年MM月dd日") + "處於" + address + "，屬於新冠疫情高風險地區，" +
                                        "請即時與該員工聯繫，確認身體狀況！");
                                emailTask.sendAsync(message);

                            } else if (judge < 1 / 3.0 && judge > 1 / 4.0) { // 中風險
                                risk = 2;
                            }

                        } else {  // 中國的方法

                            String code = cityDao.searchCode(city);

                            String url = "http://m." + code + ".bendibao.com/news/yqdengji/?qu=" + district;
                            Document document = Jsoup.connect(url).get();
                            Elements elements = document.getElementsByClass("list-content");
                            if (elements.size() > 0) {
                                Element element = elements.get(0); // 取得第一個出現的list-content
                                String result = element.select("p:last-child").text(); // 取得最後一個p
                                if ("高风险".equals(result)) {
                                    risk = 3;
                                    // 發送警告郵件
                                    HashMap<String, String> map = userDao.searchNameAndDept(userId);
                                    String name = map.get("name");
                                    String deptName = map.get("dept_name");
                                    deptName = deptName != null ? deptName : "";
                                    SimpleMailMessage message = new SimpleMailMessage();
                                    message.setTo(hrEmail);
                                    message.setSubject("員工" + name + "身處高風險疫情第區警告");
                                    message.setText(deptName + "員工" + name + "，" + DateUtil.format(new Date(),
                                            "yyyy年MM月dd日") + "處於" + address + "，屬於新冠疫情高風險地區，" +
                                            "請即時與該員工聯繫，確認身體狀況！");
                                    emailTask.sendAsync(message);

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
                // PO對象，為什麼不用Autowired? 簡單的POJO對象的賦值是由數據庫做的，不需要Spring框架託管
                TbCheckin entity = new TbCheckin();
                entity.setUserId(userId);
                entity.setAddress(address);
                entity.setCountry(country);
                entity.setProvince(province);
                entity.setCity(city);
                entity.setDistrict(district);
                entity.setStatus((byte) status);
                entity.setDate(DateUtil.today()); // DateUtil.today()返回String日期
                entity.setCreateTime(d1);
                entity.setRisk(risk);
                checkinDao.insert(entity);

            }
        }
    }

    @Override
    public void createFaceModel(int userId, String path) {

        HttpRequest request = HttpUtil.createPost(createFaceModelUrl);
        request.form("photo", FileUtil.file(path));
        request.form("code", code); // 資源提取碼
        HttpResponse response = request.execute();
        String body = response.body();
        if ("无法辨识出人脸".equals(body) || "照片中存在多张人脸".equals(body)) {
            throw new EmosException(body);
        } else {
            TbFaceModel entity = new TbFaceModel();
            entity.setUserId(userId);
            entity.setFaceModel(body);
            faceModelDao.insert(entity);
        }
    }

    @Override
    public HashMap searchTodayCheckin(int userId) {
        HashMap map = checkinDao.searchTodayCheckin(userId);
        return map;
    }

    @Override
    public long searchCheckinDays(int userId) {
        long days = checkinDao.searchCheckinDays(userId);
        return days;
    }

    @Override
    public ArrayList<HashMap> searchWeekCheckin(HashMap param) { // {startDate:"" ,endDate:"" ,userId:""}

        ArrayList<HashMap> checkinList = checkinDao.searchWeekCheckin(param); // {[date:"2023-2-20",status:"正常"],[date:"2023-2-21",status:"正常"],...}
        ArrayList holidaysList = holidaysDao.searchHolidaysInRange(param);
        ArrayList workdayList = workdayDao.searchWorkdayInRange(param);

        DateTime startDate = DateUtil.parseDate(param.get("startDate").toString());
        DateTime endDate = DateUtil.parseDate(param.get("endDate").toString());
        DateRange range = DateUtil.range(startDate, endDate, DateField.DAY_OF_MONTH); // 會返回一個月的紀錄 2021-06-18 21:39:21...，但這裡end剛好是一週後所以返回七天
        ArrayList<HashMap> list = new ArrayList<>();

        range.forEach(one -> {
            String date = one.toString("yyyy-MM-dd");
            String type = "工作日";
            if (one.isWeekend()) {
                type = "節假日";
            }
            if (holidaysList != null && holidaysList.contains(date)) {
                type = "節假日";
            } else if (workdayList != null && workdayList.contains(date)) {
                type = "工作日";
            }
            String status = ""; // 比如現在禮拜三，但你要查禮拜五，查不到東西status為空
            if (type.equals("工作日") && DateUtil.compare(one, DateUtil.date()) <= 0) { // one 和 DateUtil.date()今天 比大小，小等於0就是在今天之前
                status = "缺勤"; // 先設定默認值，如果下面找不到就是缺勤
                boolean flag = false; // 還沒考勤
                for (HashMap<String, String> map : checkinList) { // 找現在的時間，找到就可以break了
                    if (map.containsValue(date)) {
                        status = map.get("status"); // map -> {[date:"2023-2-20",status:"正常"]}
                        flag = true; // 代表今天已經考勤過了，已經有拿到status了，那下面的特殊情況就不會存在
                        break; // 但外面forEach()不會跟著結束
                    }
                }

                // DateTime -> DateUtil.date() like 2021-07-01 21:32:47 代表現在
                // String -> DateUtil.today() like 2021-07-01 代表現在
                DateTime endTime = DateUtil.parse(DateUtil.today() + " " + constants.attendanceEndTime); // 考勤結束的時間
                String today = DateUtil.today(); // ex: 2023-2-20
                // 特殊狀況：如果查到的是今天，但今天還沒考勤，且今天還沒過考勤時間
                if (date.equals(today) && DateUtil.date().isBefore(endTime) && flag == false) {
                    status = ""; // 如果現在還沒到考勤結束時間，且今天還沒考勤，查了status會是空
                }

            }
            HashMap map = new HashMap();
            map.put("date", date);
            map.put("status", status);
            map.put("type", type);
            map.put("day", one.dayOfWeekEnum().toChinese("週")); // 星期一 => 週一
            list.add(map);

        });
        return list;
    }

    @Override
    public ArrayList<HashMap> searchMonthCheckin(HashMap param) {
        return this.searchWeekCheckin(param);
    }
}

