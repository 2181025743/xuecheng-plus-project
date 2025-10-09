package com.xuecheng.content.model.dto;

import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.model.po.TeachplanMedia;
import lombok.Data;
import lombok.ToString;

import java.util.List;

/**
 * 课程计划树形结构DTO
 * 
 * 用于返回课程计划的树形数据
 * 继承Teachplan PO类，并扩展两个属性：
 * 1. teachplanMedia：关联的媒资信息（只有小节才有）
 * 2. teachPlanTreeNodes：子节点列表（只有大章节才有）
 */
@Data
@ToString
public class TeachplanDto extends Teachplan {

    /**
     * 课程计划关联的媒资信息
     * 
     * 说明：
     * - 只有小节（grade=2）才有媒资信息
     * - 大章节（grade=1）此字段为null
     * - 通过teachplan_media表关联查询得到
     */
    private TeachplanMedia teachplanMedia;

    /**
     * 子节点列表（小章节）
     * 
     * 说明：
     * - 只有大章节（grade=1）才有子节点
     * - 小节（grade=2）此字段为空数组[]
     * - 类型是List<TeachplanDto>，形成递归树形结构
     */
    private List<TeachplanDto> teachPlanTreeNodes;
}
