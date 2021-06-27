package com.example.emos.wx.service.SmallTools;


public class ChineseConverter {

    public static String CHconverter(String word) {
        if (word.equals("台北市")) {
            return "台北市";
        } else if (word.equals("新北市")) {
            return "新北市";
        } else if (word.equals("基隆市")) {
            return "基隆市";
        } else if (word.equals("宜兰县")) {
            return "宜蘭縣";
        } else if (word.equals("桃园市")) {
            return "桃園市";
        } else if (word.equals("新竹市")) {
            return "新竹市";
        } else if (word.equals("新竹县")) {
            return "新竹縣";
        } else if (word.equals("苗栗县")) {
            return "苗栗縣";
        } else if (word.equals("台中市")) {
            return "台中市";
        } else if (word.equals("彰化县")) {
            return "彰化縣";
        } else if (word.equals("南投县")) {
            return "南投縣";
        } else if (word.equals("云林县")) {
            return "雲林縣";
        } else if (word.equals("嘉义市")) {
            return "嘉義市";
        } else if (word.equals("嘉义县")) {
            return "嘉義縣";
        } else if (word.equals("台南市")) {
            return "台南市";
        } else if (word.equals("高雄市")) {
            return "高雄市";
        } else if (word.equals("屏东县")) {
            return "屏東縣";
        } else if (word.equals("花莲县")) {
            return "花蓮縣";
        } else if (word.equals("台东县")) {
            return "台東縣";
        } else if (word.equals("澎湖县")) {
            return "澎湖縣";
        } else if (word.equals("连江县")) {
            return "連江縣";
        } else if (word.equals("金门县")) {
            return "金門縣";
        }
        return null;
    }
}