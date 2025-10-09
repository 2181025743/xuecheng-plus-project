package com.xuecheng.content.model.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import lombok.Data;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * <p>
 * 课程营销信息
 * </p>
 *
 * @author itcast
 */
@Data
@TableName("course_market")
public class CourseMarket implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键，课程id
     * 注意：这个ID不是自增的，而是使用course_base表的ID
     * 使用INPUT策略，表示手动设置ID值
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * 收费规则，对应数据字典
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
     * 咨询qq
     */
    private String qq;

    /**
     * 微信
     */
    private String wechat;

    /**
     * 电话
     */
    private String phone;

    /**
     * 有效期天数
     */
    private Integer validDays;

}
