package com.example.emos.wx.controller;


import com.example.emos.wx.common.util.R;
import com.example.emos.wx.controller.form.TestSayHelloForm;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;


@RestController // JSON
@RequestMapping("/test")
@Api(tags = "測試Web接口")
public class TestController {

    @PostMapping("/sayHello")
    @ApiOperation("最簡單的測試方法")  // 方便swagger網頁上顯示
    public R sayHello(@Valid @RequestBody TestSayHelloForm form) { // 如果這裡通過，下面就可以執行
        return R.ok().put("message", "Hello, " + form.getName()); // R對象寫到響應體裡
    }


    @PostMapping("/addUser")
    @ApiOperation("添加用戶")
    @RequiresPermissions(value = {"ROOT", "USER:ADD"}, logical = Logical.OR) // 後面logic表示前面ROOT或USER:ADD都可以
    public R addUser(){
        return R.ok("用戶添加成功");
    }

}


