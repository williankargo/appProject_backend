package com.example.emos.wx.service;

// 因為service很容易變動，所以用實現接口子類的方式來實現服務

import java.util.Set;

public interface UserService {

    public int registerUser(String registerCode, String code, String nickname, String photo);

    public Set<String> searchUserPermissions(int userId);

    public Integer login(String code);
}
