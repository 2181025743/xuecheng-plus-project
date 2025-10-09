package com.xuecheng.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import com.xuecheng.content.model.po.CourseMarket;

import java.util.List;

/**
 * <p>
 * 课程营销信息 Mapper 接口
 * </p>
 *
 * @author itcast
 */
public interface CourseMarketMapper extends BaseMapper<CourseMarket> {
    public List<CourseCategoryTreeDto> selectTreeNodes(String id);
}
