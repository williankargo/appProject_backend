package com.example.emos.wx.config.shiro;


import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpStatus;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.web.filter.authc.AuthenticatingFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

// springMVC的filter
// 客戶端每次的請求都會被filter攔截，後端發出的響應也會被filter攔截
// todo: 如果是單例的，那多個thread使用OAuth2Filter時，用的ThreadLocalToken就是同一個對象，會有thread安全問題。
@Component // 現在只是普通的bean，需經過shiroConfig註冊給shiro框架才能發揮filter的功能
@Scope("prototype") // 每次調用或請求這個bean都會創建一個新的實例，往threadLocal裡面保存數據才不會出現問題。
public class OAuth2Filter extends AuthenticatingFilter {

    @Autowired
    private ThreadLocalToken threadLocalToken;

    @Value("${emos.jwt.cache-expire}")
    private int cacheExpire;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    protected AuthenticationToken createToken(ServletRequest request, ServletResponse response) throws Exception {
        HttpServletRequest req = (HttpServletRequest) request;
        String token = getRequestToken(req); // 獲取token
        if (StrUtil.isBlank(token)) {
            return null;
        }
        return new OAuth2Token(token);
    }


    /**
     * 攔截請求，判斷請求是否需要被shiro處理
     */
    // Ajax提交application/json數據的時候，會先發出Options請求試探，再發送資料
    @Override
    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {

        // HttpServletRequest繼承自ServletRequest，多了一些針對http協議的方法
        HttpServletRequest req = (HttpServletRequest) request;

        // 放行Options請求，不需要Shiro處理
        if (req.getMethod().equals(RequestMethod.OPTIONS.name())) {
            return true;
        }
        // 其他所有請求都要被Shiro處理
        return false;
    }


    /**
     * 該方法用於處理所有應該被Shiro處理的請求
     */
    @Override
    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        resp.setHeader("Content-Type", "text/html;charset=UTF-8");

        // 允許跨域請求(因為前後端分離) //
        resp.setHeader("Access-Control-Allow-Origin", req.getHeader("Origin"));
        // 當server收到跨跨域請求時，會依據http header附的origin值決定是否要同意，只要在response加上ACAO就可以授權同意請求了
        resp.setHeader("Access-Control-Allow-Credentials", "true"); // 允許跨域請求攜帶cookies傳進來

        threadLocalToken.clear(); // 把上個請求的token緩存刪掉
        String token = getRequestToken((HttpServletRequest) request);
        if(StringUtils.isBlank(token)){
            resp.setStatus(HttpStatus.HTTP_UNAUTHORIZED);
            resp.getWriter().print("無效的token#1");
            return false;
        }

        try{
            jwtUtil.verifierToken(token); // 檢查token是否過期
        }catch(TokenExpiredException e){
            // 客戶端令牌過期，查詢Redis中是否存在token，如果存在token就重新生成一個token給客戶端

            if(redisTemplate.hasKey(token)){

                redisTemplate.delete(token); // 準備重新更新redis裡的令牌
                int userId = jwtUtil.getUserId(token);
                token = jwtUtil.createToken(userId);

                // 把新token保存到redis中
                redisTemplate.opsForValue().set(token, userId + "", cacheExpire, TimeUnit.DAYS);
                // 把新令牌綁定到threadLocalToken中
                threadLocalToken.setToken(token);
            }else{
                // 如果Redis不存在token，讓用戶重新登入
                resp.setStatus(HttpStatus.HTTP_UNAUTHORIZED);
                resp.getWriter().print("token已過期");
                return false;
            }

        }catch(JWTDecodeException e){
            resp.setStatus(HttpStatus.HTTP_UNAUTHORIZED);
            resp.getWriter().print("無效的token#2");
            return false;
        }

        // 1. 以上為授權流程：token(有了機票)
        // 2. 認證流程：(還要登機證)，executeLogin創建『認證對象』然後綁定到請求上，Shiro才會放行請求，才能執行web方法
        boolean bool = executeLogin(request, response);
        return bool;
    }

    // 2. 認證流程失敗，會被executeLogin調用
    @Override
    protected boolean onLoginFailure(AuthenticationToken token, AuthenticationException e, ServletRequest request, ServletResponse response) {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        resp.setHeader("Content-Type", "text/html;charset=UTF-8");
        resp.setHeader("Access-Control-Allow-Origin", req.getHeader("Origin"));
        resp.setHeader("Access-Control-Allow-Credentails", "true");
        resp.setStatus(HttpStatus.HTTP_UNAUTHORIZED);
        try{
            resp.getWriter().print(e.getMessage());
        }catch(Exception ee){}
        return false;
    }

    /**
     * 獲取請求頭裡面的token
     */
    private String getRequestToken(HttpServletRequest request) {

        // 從header中獲取token
        String token = request.getHeader("token");

        // 如果header中不存在token，則從參數中獲取token
        if (StringUtils.isBlank(token)) {
            token = request.getParameter("token");
        }
        return token;
    }

    // 這個filter類中的do filter方法
    @Override
    public void doFilterInternal(ServletRequest request, ServletResponse response, FilterChain chain) throws ServletException, IOException {
        super.doFilterInternal(request, response, chain);
    }
}
