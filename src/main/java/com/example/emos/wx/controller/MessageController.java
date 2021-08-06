package com.example.emos.wx.controller;


import com.example.emos.wx.common.util.R;
import com.example.emos.wx.config.shiro.JwtUtil;
import com.example.emos.wx.controller.form.DeleteMessageRefByIdForm;
import com.example.emos.wx.controller.form.SearchMessageByIdForm;
import com.example.emos.wx.controller.form.SearchMessageByPageForm;
import com.example.emos.wx.controller.form.UpdateUnreadMessageForm;
import com.example.emos.wx.service.MessageService;
import com.example.emos.wx.task.MessageTask;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/message")
@Api(tags = "消息模塊網路接口")
public class MessageController {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private MessageService messageService;

    @Autowired
    private MessageTask messageTask;

    @PostMapping("/searchMessageByPage")
    @ApiOperation("獲取分頁消息列表")
    public R searchMessageByPage(@Valid @RequestBody SearchMessageByPageForm form, @RequestHeader("token") String token) {
        int userId = jwtUtil.getUserId(token);
        int page = form.getPage();
        int length = form.getLength();

        long start = (long) (page - 1) * length; // 第一頁, page = 1, start從0開始
        List<HashMap> list = messageService.searchMessageByPage(userId, start, length);
        return R.ok().put("result", list);
    }

    @PostMapping("/searchMessageById")
    @ApiOperation("根據ID查詢消息")
    public R searchMessageById(@Valid @RequestBody SearchMessageByIdForm form) {
        HashMap map = messageService.searchMessageById(form.getId());
        return R.ok().put("result", map);
    }

    @PostMapping("/updateUnreadMessage")
    @ApiOperation("未讀消息更新成已讀消息")
    public R updateUnreadMessage(@Valid @RequestBody UpdateUnreadMessageForm form) {
        System.out.println("update!!!");
        long rows = messageService.updateUnreadMessage(form.getId());
        return R.ok().put("result", rows > 1 ? true : false);
    }

    @PostMapping("/deleteMessageRefById")
    @ApiOperation("刪除消息")
    public R deleteMessageRefById(@Valid @RequestBody DeleteMessageRefByIdForm form) {
        long rows = messageService.deleteMessageRefById(form.getId());
        return R.ok().put("result", rows > 1 ? true : false);
    }

    @GetMapping("/refreshMessage")
    @ApiOperation("刷新用戶消息")
    public R refreshMessage(@RequestHeader("token") String token) {

        int userId = jwtUtil.getUserId(token);
        messageTask.receiveAsync(userId + ""); // 異步執行，還沒執行完 程式也可以往下走

        long lastRows = messageService.searchLastCount(userId); // 因為上面是異步執行，所以這裡返回的值可能是上一次得到的
        long unreadRows = messageService.searchUnreadCount(userId);

        return R.ok().put("lastRows", lastRows).put("unreadRows", unreadRows);
    }
}
