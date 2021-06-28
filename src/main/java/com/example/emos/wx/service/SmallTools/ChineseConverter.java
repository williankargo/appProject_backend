package com.example.emos.wx.service.SmallTools;

// 不需要實現什麼特殊功能所以不需要加註解託管給spring
public class ChineseConverter {

    public static String CHconverter(String word) {
        if (word.equals("台北市")) {
            return "台北市";
        } else if ("新北市".equals(word)) {
            return "新北市";
        } else if ("基隆市".equals(word)) {
            return "基隆市";
        } else if ("宜兰县".equals(word)) {
            return "宜蘭縣";
        } else if ("桃园市".equals(word)) {
            return "桃園市";
        } else if ("新竹市".equals(word)) {
            return "新竹市";
        } else if ("新竹县".equals(word)) {
            return "新竹縣";
        } else if ("苗栗县".equals(word)) {
            return "苗栗縣";
        } else if ("台中市".equals(word)) {
            return "台中市";
        } else if ("彰化县".equals(word)) {
            return "彰化縣";
        } else if ("南投县".equals(word)) {
            return "南投縣";
        } else if ("云林县".equals(word)) {
            return "雲林縣";
        } else if ("嘉义市".equals(word)) {
            return "嘉義市";
        } else if ("嘉义县".equals(word)) {
            return "嘉義縣";
        } else if ("台南市".equals(word)) {
            return "台南市";
        } else if ("高雄市".equals(word)) {
            return "高雄市";
        } else if ("屏东县".equals(word)) {
            return "屏東縣";
        } else if ("花莲县".equals(word)) {
            return "花蓮縣";
        } else if ("台东县".equals(word)) {
            return "台東縣";
        } else if ("澎湖县".equals(word)) {
            return "澎湖縣";
        } else if ("连江县".equals(word)) {
            return "連江縣";
        } else if ("金门县".equals(word)) {
            return "金門縣";
        }
        return null;
    }
}