package com.example.emos.wx.exception;

import lombok.Data;


// 精簡異常
@Data  // 自動生成getter setter
public class EmosException extends RuntimeException{ // runtimeexception可以自動處理
    private String msg;
    private int code = 500; // 狀態碼寫死500

    public EmosException(String msg) {
        super(msg);
        this.msg = msg;
    }

    public EmosException(String msg, Throwable e) {
        super(msg, e);
        this.msg = msg;
    }

    public EmosException(String msg, int code) {
        super(msg);
        this.msg = msg;
        this.code = code;
    }

    public EmosException(String msg, int code, Throwable e) {
        super(msg, e);
        this.msg = msg;
        this.code = code;
    }
}

