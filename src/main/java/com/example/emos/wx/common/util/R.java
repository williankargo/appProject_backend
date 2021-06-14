package com.example.emos.wx.common.util;

import org.apache.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

public class R extends HashMap<String, Object> { // extends HashMap後代表R變成了HashMap類型
    public R() {
        // 狀態碼
        put("code", HttpStatus.SC_OK);
        // 業務消息
        put("msg", "success");
    }

    public static R error() {
        return error(HttpStatus.SC_INTERNAL_SERVER_ERROR, "未知異常，請聯繫管理員");
    }

    public static R error(String msg) {
        return error(HttpStatus.SC_INTERNAL_SERVER_ERROR, msg);
    }

    public static R error(int code, String msg) {
        R r = new R();
        r.put("code", code);
        r.put("msg", msg);
        return r;
    }

    public static R ok(String msg) {
        R r = new R();
        r.put("msg", msg);
        return r;
    }

    public static R ok(Map<String, Object> map) {
        R r = new R();
        r.putAll(map);
        return r;
    }

    public static R ok() {
        return new R();
    }

    // 鏈式調用，return R(自己)，方便連續調用其他方法，看其他class才能體會到
    // ex: return R.ok().put("message", "Hello, " + form.getName());
    public R put(String key, Object value) {
        super.put(key, value);
        return this;
    }
}


