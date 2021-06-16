package com.example.emos.wx.aop;

import com.example.emos.wx.common.util.R;
import com.example.emos.wx.config.shiro.ThreadLocalToken;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class TokenAspect {

    @Autowired
    private ThreadLocalToken threadLocalToken; // 媒界

    // where
    @Pointcut("execution(public * com.example..emos.wx.controller.*.*(..))") // 這裡聲明的都要被攔截
    public void aspect() {

    }

    // when
    @Around("aspect()") // 參數為給上面的切點方法通知，@Around: after before方法執行都要攔截
    public Object around(ProceedingJoinPoint point) throws Throwable {
        R r = (R) point.proceed(); // 返回方法執行結果

        String token = threadLocalToken.getToken();
        // 不為空代表傳進來時過期了，已經被redis中的緩存更新了，所以要傳新token給客戶端
        if (token != null) {
            r.put("token", token);
            threadLocalToken.clear();
        }
        return r;
    }
}
