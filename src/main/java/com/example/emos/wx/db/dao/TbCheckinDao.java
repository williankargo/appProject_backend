package com.example.emos.wx.db.dao;

import org.apache.ibatis.annotations.Mapper;

import java.util.HashMap;


@Mapper
public interface TbCheckinDao {

    public Integer haveCheckin(HashMap param);
}