package com.xuecheng.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import com.xuecheng.content.model.po.CourseCategory;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 课程分类 Mapper 接口
 * </p>
 *
 * @author itcast
 */
public interface CourseCategoryMapper extends BaseMapper<CourseCategory> {

    /**
     * 使用递归查询课程分类树形结构
     * 从指定的根节点开始，递归查询所有子节点
     * 
     * @param id 根节点ID
     * @return 课程分类树形列表（包含所有层级的节点，但未组装父子关系）
     */
    List<CourseCategoryTreeDto> selectTreeNodes(@Param("id") String id);

}
