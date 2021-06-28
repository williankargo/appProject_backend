package com.example.emos.wx.db.pojo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * tb_checkin
 * @author 
 */

// PO
@Data
public class TbCheckin implements Serializable {
    /**
     * 主键
     */
    private Integer id;

    /**
     * 用户ID
     */
    private Integer userId;

    /**
     * 签到地址
     */
    private String address;

    /**
     * 国家
     */
    private String country;

    /**
     * 省份
     */
    private String province;

    /**
     * 城市
     */
    private String city;

    /**
     * 区划
     */
    private String district;

    /**
     * 考勤结果
     */
    private Byte status;

    /**
     * 风险等级
     */
    private Integer risk;

    /**
     * 签到日期
     */
    private String date;

    // 數據表中的date是DateTime類型，java裡沒有DateTime類型，
    // 於是ORM映射就抓java裡的Date來接，但是不想保留時分秒的資訊，於是用String來接，只保留日期數據

    /**
     * 签到时间
     */
    private Date createTime;

    private static final long serialVersionUID = 1L;
}
