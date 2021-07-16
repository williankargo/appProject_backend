package com.example.emos.wx.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.example.emos.wx.db.dao.TbUserDao;
import com.example.emos.wx.db.pojo.MessageEntity;
import com.example.emos.wx.db.pojo.TbUser;
import com.example.emos.wx.exception.EmosException;
import com.example.emos.wx.service.UserService;

import com.example.emos.wx.task.MessageTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Set;


// 實現Service子類
@Service
@Slf4j
@Scope("prototype")  // 多例, 因為token刷新
public class UserServiceImpl implements UserService {

    @Value("${wx.app-id}")
    private String appId;

    @Value("${wx.app-secret}")
    private String appSecret;

    @Autowired
    private TbUserDao userDao;

    @Autowired
    private MessageTask messageTask;

    // 去微信取得openId(用戶唯一標示)
    private String getOpenId(String code) { // code: 臨時授權字符串
        String url = "https://api.weixin.qq.com/sns/jscode2session";
        HashMap map = new HashMap();
        map.put("appid", appId);
        map.put("secret", appSecret);
        map.put("js_code", code);
        map.put("grant_type", "authorization_code");

        String response = HttpUtil.post(url, map);
        JSONObject json = JSONUtil.parseObj(response);

        String openId = json.getStr("openid");
        if (openId == null || openId.length() == 0) {
            throw new RuntimeException("臨時登入憑證錯誤"); // RunTimeException() 用在微信平台出現問題
        }
        return openId;
    }

    @Override
    public int registerUser(String registerCode, String code, String nickname, String photo) {

        // 如果邀請碼是00000，代表是超級管理員
        if (registerCode.equals("000000")) {
            // 查詢超級管理員帳戶是否已經綁定
            boolean bool = userDao.haveRootUser();
            if (!bool) {
                // 把當前用戶綁定到ROOT帳戶
                String openId = getOpenId(code);
                HashMap param = new HashMap();

                param.put("openId", openId);
                param.put("nickname", nickname);
                param.put("photo", photo);
                param.put("role", "[0]");
                param.put("status", 1);
                param.put("createTime", new Date());
                param.put("root", true);

                userDao.insert(param);
                int id = userDao.searchIdByOpenId(openId);

                MessageEntity entity = new MessageEntity();
                entity.setSenderId(0);
                entity.setSenderName("系統消息");
                entity.setUuid(IdUtil.simpleUUID());
                entity.setMsg("歡迎您註冊成為超級管理員，請即時更新您的個人員工信息");
                entity.setSendTime(new Date());
                messageTask.sendAsync(id + "", entity);

                return id;
            } else {
                // 如果root已經綁定了，就拋出異常
                throw new EmosException("無法綁定超級管理員帳號"); // 如果是Service層出現異常，返回EmosException
            }
        } else {
// todo
        }

        return 0;
    }

    @Override
    public Set<String> searchUserPermissions(int userId) {
        Set<String> permissions = userDao.searchUserPermissions(userId);
        return permissions;
    }

    @Override
    public Integer login(String code) {
        String openId = getOpenId(code);
        Integer id = userDao.searchIdByOpenId(openId);
        if (id == null) {
            throw new EmosException("帳戶不存在");
        }
        // 從消息隊列中接收消息，轉移到消息表
        messageTask.receiveAsync(id + "");
        return id;
    }

    @Override
    public TbUser searchById(int userId) {
        TbUser user = userDao.searchById(userId);
        return user;
    }

    @Override
    public String searchUserHiredate(int userId) {

        String hiredate = userDao.searchUserHiredate(userId);
        return hiredate;
    }

    @Override
    public HashMap searchUserSummary(int userId) {
        HashMap map = userDao.searchUserSummary(userId);
        return map;
    }
}
