package com.xuecheng.content.model.dto;

import com.xuecheng.content.model.po.CourseCategory;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 课程分类树形结构DTO
 * 用于返回课程分类的树形数据
 * 
 * 继承CourseCategory，复用数据库字段
 * 新增childrenTreeNodes属性，用于存储子节点
 */
@Data
public class CourseCategoryTreeDto extends CourseCategory implements Serializable {

    /**
     * 下级节点列表
     * 存储当前分类的所有子分类
     * 类型是 List<CourseCategoryTreeDto>，形成递归树形结构
     */
    private List<CourseCategoryTreeDto> childrenTreeNodes;

}
