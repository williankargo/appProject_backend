package com.example.emos.wx.controller.form;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@ApiModel
@Data // getter setter
public class TestSayHelloForm {

//    @NotBlank
//    @Pattern(regexp = "^[\\u4e00-\\u9fa5]{2,15}$") // 漢字範圍，2個到15個字
    @ApiModelProperty("姓名") // 方便swagger網頁上顯示
    private String name;
}


