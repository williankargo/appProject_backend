package com.example.emos.wx.db.dao;

import org.apache.ibatis.annotations.Mapper;

import java.util.HashMap;
import java.util.Set;

@Mapper
public interface TbUserDao {
    public boolean haveRootUser();

    public int insert(HashMap param); // 返回int得知有沒有插入成功

    public Integer searchIdByOpenId(String openId);

    public Set<String> searchUserPermissions(int userId);
}
