package com.example.emos.wx.controller.form;


import io.swagger.annotations.ApiModel;
import lombok.Data;

// VO
@Data
@ApiModel
public class CheckinForm {
    private String address;
    private String country; // nation or country?
    private String province;
    private String city;
    private String district;
}

