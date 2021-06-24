package com.example.emos.wx.config.shiro;


import com.example.emos.wx.db.pojo.TbUser;
import com.example.emos.wx.service.UserService;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

// 小程序 -> Ajax -> XssFilter -> OAuth2Filter檢查token是否合法 ->
// OAuth2Realm認證，頒發認證對象 ->
// OAuth2Realm授權 (和JWT已無關，和RBAC有關)
// -> 才可以到Web方法

@Component
public class OAuth2Realm extends AuthorizingRealm {

    @Autowired // 注入JWT token
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof OAuth2Token; // 看是不是要求的封裝對象
    }

    /**
     * 授權方法（驗證權限時調用）
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection collection) {

        TbUser user = (TbUser) collection.getPrimaryPrincipal();  // 獲得用戶基本信息
        int userId = user.getId();
        Set<String> permsSet = userService.searchUserPermissions(userId);
        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo(); // 認證對象
        info.setStringPermissions(permsSet); // 把權限列表添加到info認證對象中
        return info;
    }

    /**
     * 認證（驗證登入時調用）
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {

        String accessToken = (String) token.getPrincipal(); // 轉成String
        int userId = jwtUtil.getUserId(accessToken);
        TbUser user = userService.searchById(userId);

        if (user == null) { // sql: status == 0
            throw new LockedAccountException("帳號已被鎖定，請聯繫管理員");
        }

        SimpleAuthenticationInfo info = new SimpleAuthenticationInfo(user, accessToken, getName()); // getName() Realm的名字
        // 此時請求被賦予了認證對象，才可以到web方法執行
        return info;
    }
}

