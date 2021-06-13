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

// 1. jwtUtil生成token字符串，传给客户端，客户端保存token字符串。
// 2. 在AuthenticatingFilter类中:
//  a. 客户端每次向后端发起请求，AuthenticatingFilter进行拦截, 通过拦截的信息(请求头或请求体)来得到客户端所带的token字符串。
//  b. 把得到的token字符串通过AuthenticationToken封装成token对象。　
//  c. 封装后的token对象传入到AuthorizingRealm中，以便于认证和授权。