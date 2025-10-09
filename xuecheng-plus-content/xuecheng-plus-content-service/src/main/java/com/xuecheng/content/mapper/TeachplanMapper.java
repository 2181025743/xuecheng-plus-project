package com.xuecheng.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.Teachplan;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 课程计划 Mapper 接口
 * </p>
 *
 * @author itcast
 */
public interface TeachplanMapper extends BaseMapper<Teachplan> {

    /**
     * 查询课程计划树形结构（使用表自连接）
     *
     * @param courseId 课程ID
     * @return 课程计划树形列表（未组装树形结构，Service层组装）
     */
    List<TeachplanDto> selectTreeNodes(@Param("courseId") Long courseId);

}
