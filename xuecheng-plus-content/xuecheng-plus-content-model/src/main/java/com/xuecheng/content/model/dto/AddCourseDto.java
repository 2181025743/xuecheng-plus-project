package com.xuecheng.content.model.dto;

import com.xuecheng.base.exception.ValidationGroups;
import com.xuecheng.content.model.po.CourseBase;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

/**
 * 添加课程DTO
 * 使用JSR 303注解进行参数校验
 */
@Data
@ApiModel(value = "AddCourseDto", description = "新增课程基本信息")
public class AddCourseDto extends CourseBase {

    // ========== 课程基本信息（必填字段） ==========
    /**
     * 课程名称
     * <p>
     * 校验规则：
     * - 新增课程时：课程名称不能为空（使用 Insert 分组）
     * - 修改课程时：课程名称不能为空（使用 Update 分组）
     * <p>
     * 说明：
     * - 同一个字段可以使用多个 @NotEmpty 注解
     * - 通过 groups 属性指定不同的校验组
     * - Controller 层使用 @Validated(ValidationGroups.Insert.class) 时，
     * 只会触发 groups 包含 Insert.class 的校验规则
     * - Controller 层使用 @Validated(ValidationGroups.Update.class) 时，
     * 只会触发 groups 包含 Update.class 的校验规则
     */
    @ApiModelProperty(value = "课程名称", required = true, example = "Java从入门到精通")
    @NotEmpty(message = "新增课程名称不能为空", groups = {ValidationGroups.Insert.class})
    @NotEmpty(message = "修改课程名称不能为空", groups = {ValidationGroups.Update.class})
    @Size(min = 2, max = 100, message = "课程名称长度为2-100个字符", groups = {ValidationGroups.Insert.class,
            ValidationGroups.Update.class})
    private String name;

    @ApiModelProperty(value = "适用人群", required = true, example = "Java初学者、编程爱好者")
    @NotEmpty(message = "适用人群不能为空", groups = {ValidationGroups.Insert.class})
    @Size(min = 10, message = "适用人群内容过少，至少10个字符", groups = {ValidationGroups.Insert.class})
    private String users;

    @ApiModelProperty(value = "课程标签", example = "Java,编程基础,后端开发")
    @NotEmpty(message = "课程标签不能为空", groups = {ValidationGroups.Insert.class})
    private String tags;

    @ApiModelProperty(value = "大分类", required = true, example = "1-3")
    @NotEmpty(message = "课程大分类不能为空", groups = {ValidationGroups.Insert.class})
    private String mt;

    @ApiModelProperty(value = "小分类", required = true, example = "1-3-2")
    @NotEmpty(message = "课程小分类不能为空", groups = {ValidationGroups.Insert.class})
    private String st;

    @ApiModelProperty(value = "课程等级", required = true, example = "200001", notes = "数据字典：200001初级/200002中级/200003高级")
    @NotEmpty(message = "课程等级不能为空", groups = {ValidationGroups.Insert.class})
    private String grade;

    @ApiModelProperty(value = "教学模式", required = true, example = "200002", notes = "数据字典：普通/录播/直播等")
    @NotEmpty(message = "教学模式不能为空", groups = {ValidationGroups.Insert.class})
    private String teachmode;

    @ApiModelProperty(value = "课程描述", example = "本课程从零开始，系统讲解Java编程语言...")
    @Size(min = 10, message = "课程描述内容过少，至少10个字符", groups = {ValidationGroups.Insert.class})
    private String description;

    @ApiModelProperty(value = "课程图片", example = "http://cdn.example.com/course.jpg")
    private String pic;

    // ========== 课程营销信息 ==========

    @ApiModelProperty(value = "收费规则", required = true, example = "201001", notes = "数据字典：201000免费/201001收费")
    @NotEmpty(message = "收费规则不能为空", groups = {ValidationGroups.Insert.class})
    private String charge;

    @ApiModelProperty(value = "现价（限时价）", example = "199.00")
    private Float price;

    @ApiModelProperty(value = "原价", example = "299.00")
    private Float originalPrice;

    @ApiModelProperty(value = "咨询QQ", example = "12345678")
    private String qq;

    @ApiModelProperty(value = "咨询微信", example = "java_teacher")
    private String wechat;

    @ApiModelProperty(value = "咨询电话", example = "13800138000")
    private String phone;

    @ApiModelProperty(value = "有效期天数", example = "365", notes = "购买后可学习的天数")
    private Integer validDays;
}
 