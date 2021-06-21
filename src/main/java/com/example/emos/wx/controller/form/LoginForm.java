package com.example.emos.wx.controller.form;


import io.swagger.annotations.ApiModel;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@ApiModel
public class LoginForm {

    @NotBlank(message = "臨時授權不能為空")
    private String code;
}
