package com.xuecheng.content.model.dto;

import com.xuecheng.content.model.po.CourseBase;
import lombok.Data;

/**
 * 课程基本信息DTO
 * 继承CourseBase，扩展营销信息字段
 * 用于返回课程的完整信息
 */
@Data
public class CourseBaseInfoDto extends CourseBase {

    /**
     * 收费规则（201000免费/201001收费）
     */
    private String charge;

    /**
     * 现价
     */
    private Float price;

    /**
     * 原价
     */
    private Float originalPrice;

    /**
     * 咨询QQ
     */
    private String qq;

    /**
     * 咨询微信
     */
    private String wechat;

    /**
     * 咨询电话
     */
    private String phone;

    /**
     * 有效期天数
     */
    private Integer validDays;
    private String mtName;
    private String stName;
    private Integer coursePubId;
    private Integer coursePubDate;
}