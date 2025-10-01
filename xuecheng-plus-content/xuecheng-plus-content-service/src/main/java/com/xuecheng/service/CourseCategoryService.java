package com.xuecheng.service;

import com.xuecheng.content.model.dto.CourseCategoryTreeDto;

import java.util.List;

/**
 * 课程分类服务接口
 */
public interface CourseCategoryService {

    /**
     * 查询课程分类树形结构
     * 
     * @param id 根节点ID
     * @return 课程分类树形列表（从一级分类开始，不包含根节点）
     */
    List<CourseCategoryTreeDto> queryTreeNodes(String id);

}
