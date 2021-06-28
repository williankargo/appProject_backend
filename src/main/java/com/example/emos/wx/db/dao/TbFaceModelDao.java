package com.example.emos.wx.db.dao;


import com.example.emos.wx.db.pojo.TbFaceModel;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TbFaceModelDao {

    public String searchFaceModel(int userId);

    public void insert(TbFaceModel faceModel); //todo: userId到底怎麼進去的？又沒有指定

    public int deleteFaceModel(int userId);
}