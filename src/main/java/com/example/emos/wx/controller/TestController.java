package com.example.emos.wx.controller;


import com.example.emos.wx.common.util.R;
import com.example.emos.wx.controller.form.TestSayHelloForm;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;


@RestController // JSON
@RequestMapping("/test")
@Api("測試Web接口")
public class TestController {

    @PostMapping("/sayHello")
    @ApiOperation("最簡單的測試方法")  // 方便swagger網頁上顯示
    public R sayHello(@Valid @RequestBody TestSayHelloForm form) { // 如果這裡通過，下面就可以執行
        return R.ok().put("message", "Hello, " + form.getName());
    }
}


