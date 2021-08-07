package com.example.emos.wx.controller.form;


import io.swagger.annotations.ApiModel;
import lombok.Data;

import javax.validation.constraints.Pattern;

@Data
@ApiModel
public class SearchUserGroupByDeptForm {
    @Pattern(regexp = "^[\\u2E80-\\u9FFF]{1,15}$")
    private String keyword;
}
