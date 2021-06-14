package com.example.emos.wx.config.shiro;

import org.apache.shiro.authc.AuthenticationToken;

// 封裝類封裝token，讓shiro好辨別
public class OAuth2Token implements AuthenticationToken { // 擴展後就變成了合格的封裝類

    private String token;

    public OAuth2Token(String token){
        this.token = token;
    }

    @Override
    public Object getPrincipal() {
        return token;
    }

    @Override
    public Object getCredentials() {
        return token;
    }
}
