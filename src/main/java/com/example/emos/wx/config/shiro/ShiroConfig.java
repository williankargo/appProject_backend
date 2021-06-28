package com.example.emos.wx.config.shiro;


import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.spring.LifecycleBeanPostProcessor;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.Filter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

// 把java類註冊給shiro框架
@Configuration
public class ShiroConfig {

    // 封裝OAuth2Realm
    @Bean("securityManager")
    public SecurityManager securityManager(OAuth2Realm realm){
        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
        securityManager.setRealm(realm);
        securityManager.setRememberMeManager(null); // 服務憑證保存在客戶端，不保存在服務端
        return securityManager;
    }

    // @Bean寫在方法上，默認是方法的駝峰字名稱，若方法有參數，spring會去容器找有沒有匹配的bean對象，像@AutoWired在做的事
    // 表明會把當前方法的返回值作為bean對象存入spring容器中
    // 封裝filter
    @Bean("shiroFilter")
    public ShiroFilterFactoryBean shiroFilter(SecurityManager securityManager, OAuth2Filter filter){
        ShiroFilterFactoryBean shiroFilter = new ShiroFilterFactoryBean();
        shiroFilter.setSecurityManager(securityManager);

        Map<String, Filter> map = new HashMap<>(); // 要創建map才能放filter
        map.put("oauth2",filter); // key隨便取
        shiroFilter.setFilters(map); // filter註冊給srpingMVC

        // 設定要攔截的項目
        Map<String, String> filterMap = new LinkedHashMap<>();
        filterMap.put("/webjars/**", "anon"); // anon代表不用攔截
        filterMap.put("/druid/**", "anon");
        filterMap.put("/app/**", "anon");
        filterMap.put("/sys/login", "anon");
        filterMap.put("/swagger/**", "anon");
        filterMap.put("/v2/api-docs", "anon");
        filterMap.put("/swagger-ui.html", "anon");
        filterMap.put("/swagger-resources/**", "anon");
        filterMap.put("/captcha.jpg", "anon");
        filterMap.put("/user/register", "anon");
        filterMap.put("/user/login", "anon");
        //filterMap.put("/test/**", "anon");
        filterMap.put("/**", "oauth2");
        shiroFilter.setFilterChainDefinitionMap(filterMap);

        return shiroFilter;
    }

    // todo: 後面用到
    // 管理shiro bean生命週期
    @Bean("lifecycleBeanPostProcessor")
    public LifecycleBeanPostProcessor lifecycleBeanPostProcessor(){
        return new LifecycleBeanPostProcessor();
    }

    // AOP切面類，Web方法執行前驗證權限。Web會返回R，往R綁定token，再返回給客戶端。
    @Bean("authorizationAttributeSourceAdvisor") // 不寫後面的string也可以，反正默認是返回class首字母小寫
    public AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor(SecurityManager securityManager){
        AuthorizationAttributeSourceAdvisor advisor = new AuthorizationAttributeSourceAdvisor();
        advisor.setSecurityManager(securityManager);
        return advisor;
    }


}




