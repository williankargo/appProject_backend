package com.example.emos.wx.controller.form;


import io.swagger.annotations.ApiModel;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;


@Data
@ApiModel
public class RegisterForm {

    @NotBlank(message = "註冊碼不能為空")
    @Pattern(regexp = "^[0-9]{6}$", message = "註冊碼必須是6位數字")
    private String registerCode;

    @NotBlank(message = "微信臨時授權不能為空")
    private String code;

    @NotBlank(message = "暱稱不能為空")
    private String nickname;

    @NotBlank(message = "頭像不能為空")
    private String photo;
}
