package com.example.emos.wx.db.dao;

import com.example.emos.wx.db.pojo.TbCheckin;
import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;
import java.util.HashMap;


@Mapper
public interface TbCheckinDao {

    public Integer haveCheckin(HashMap param);

    public void insert(TbCheckin checkin);

    public HashMap searchTodayCheckin(int userId);

    public long searchCheckinDays(int userId);

    public ArrayList<HashMap> searchWeekCheckin(HashMap param); //因為會返回多個hashmap所以用arraylist接住(要自己判斷)
}