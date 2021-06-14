package com.example.emos.wx.config.shiro;


import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OAuth2Realm extends AuthorizingRealm {

    @Autowired // 注入JWT token
    private JwtUtil jwtUtil;


    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof OAuth2Token; // 看是不是要求的封裝對象
    }

    /**
     * 授權方法（驗證權限時調用）
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {

        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo(); // 認證對象
        //TODO 查詢用戶權限列表
        //TODO 把權限列表添加到info對象中
        return info;
    }

    /**
     * 認證（登陸時調用）
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        //TODO 從令牌中獲取userId，然後檢測該帳戶是否被凍結。
        SimpleAuthenticationInfo info = new SimpleAuthenticationInfo();
        //TODO 往info對象中添加用户信息、Token字符串
        return info;
    }
}

