package com.example.emos.wx.config.shiro;

import org.springframework.stereotype.Component;

// 給每個線程自己的更衣室
@Component
public class ThreadLocalToken {

    private ThreadLocal<String> local = new ThreadLocal<>();

    public void setToken(String token) {
        local.set(token);
    }

    public String getToken() {
        return (String) local.get();
    }

    public void clear() {
        local.remove();
    }
}
