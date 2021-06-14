package com.example.emos.wx.config.shiro;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@Slf4j // 日誌
public class JwtUtil {

    @Value("${emos.jwt.secret}")
    private String secret;

    @Value("${emos.jwt.expire}")
    private int expire;


    // 生成 JWT token
    public String createToken(int userId){
        Date date = DateUtil.offset(new Date(), DateField.DAY_OF_YEAR, 5); // 從今天算起偏移5天
        Algorithm algorithm = Algorithm.HMAC256(secret);  // 加密的方法

        JWTCreator.Builder builder = JWT.create();
        String token = builder.withClaim("userId", userId).withExpiresAt(date).sign(algorithm);
        return token;
    }

    public int getUserId(String token){
        DecodedJWT jwt = JWT.decode(token);
        int userId = jwt.getClaim("userId").asInt();
        return userId;
    }

    public void verifierToken(String token){
        Algorithm algorithm = Algorithm.HMAC256(secret);
        JWTVerifier verifier = JWT.require(algorithm).build();
        verifier.verify(token); // 如果驗證不過會拋出RuntimeException(不需要捕獲)
    }

}

//  Shiro是認證和授權(區分級別)的框架, JWT是用来實現單點登陸，生成令牌
// 1. jwtUtil生成token字符串，傳给客户端，客户端保存token字符串。
// 2. 在AuthenticatingFilter類中:
//  a. 客户端每次向後端發起請求，AuthenticatingFilter進行攔截, 通過攔截的信息(請求頭或請求體)来得到客户端所带的token字符串。
//  b. 把得到的token字符串通過AuthenticationToken封装成token對象。　
//  c. 封裝後的token對象傳入到AuthorizingRealm(Shiro)中，以便於認證和授權。
