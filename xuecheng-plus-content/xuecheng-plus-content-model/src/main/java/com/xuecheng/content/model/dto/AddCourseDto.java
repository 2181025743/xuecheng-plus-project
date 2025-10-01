package com.xuecheng.content.model.dto;

import com.xuecheng.content.model.po.CourseBase;
import lombok.Data;

@Data
public class AddCourseDto extends CourseBase {
    private String mt;
    private String st;
    private String name;
    private String pic;
    private String teachmode;
    private String users;
    private String tags;
    private String grade;
    private String description;
    private String charge;
    private Float price;
    private Float originalPrice;
    private String qq;
    private String wechat;
    private String phone;
    private Integer validDays;
}
