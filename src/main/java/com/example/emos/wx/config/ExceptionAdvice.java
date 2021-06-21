package com.example.emos.wx.config;

import com.example.emos.wx.exception.EmosException;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.UnauthorizedException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// 全局捕獲的異常攔截器
@Slf4j // 日誌模塊
@RestControllerAdvice // 捕獲springMVC所有異常(因為是用Restful概念開發所以前面加上Rest)
public class ExceptionAdvice {

    @ResponseBody // 要寫到響應裡面的
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR) // 設定狀態碼500
    @ExceptionHandler(Exception.class) // 加上後才能捕獲Exception異常
    public String ExceptionHandler(Exception e){

        log.error("執行異常",e);
        if(e instanceof MethodArgumentNotValidException){
            MethodArgumentNotValidException exception = (MethodArgumentNotValidException) e;

            // 不用getMessage() 太冗長了
            // 將錯誤訊息返回給前台
            return exception.getBindingResult().getFieldError().getDefaultMessage();
        }
        else if(e instanceof EmosException){
            EmosException exception = (EmosException) e;
            return exception.getMsg();
            }
        else if(e instanceof UnauthorizedException){
            return "你不具備相關權限";
        }else{

            // 如果異常發生在service而不是controller，沒有R對象可以包裹進響應體，這裡直接傳給響應體就好
            return "後端執行異常";
        }
    }
}
