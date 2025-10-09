package com.xuecheng.content.model.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import java.io.Serializable;
import lombok.Data;
import com.baomidou.mybatisplus.annotation.TableName;

@Data
@TableName("course_base")
public class CourseBase implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long companyId;

    private String companyName;

    private String name;

    private String users;

    private String tags;

    private String mt;

    private String st;

    private String grade;

    private String teachmode;

    private String description;

    private String pic;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createDate;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime changeDate;

    private String createPeople;

    private String changePeople;

    private String auditStatus;

    private String status;


}
