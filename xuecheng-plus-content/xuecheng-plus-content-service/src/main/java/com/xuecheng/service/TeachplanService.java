package com.xuecheng.service;

import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;

import java.util.List;

/**
 * 课程计划管理Service
 */
public interface TeachplanService {

    /**
     * 根据课程id查询课程计划树形结构
     *
     * @param courseId 课程ID
     * @return 课程计划树形列表
     */
    List<TeachplanDto> findTeachplanTree(Long courseId);

    /**
     * 新增/修改/保存课程计划
     * <p>
     * 功能说明：
     * 1. 新增大章节：dto.id=null, parentid=0, grade=1
     * 2. 新增小章节：dto.id=null, parentid=父章节id, grade=2
     * 3. 修改章节：dto.id不为null，更新对应的章节信息
     *
     * @param saveTeachplanDto 课程计划信息
     */
    void saveTeachplan(SaveTeachplanDto saveTeachplanDto);

    /**
     * 删除课程计划
     * <p>
     * 业务规则：
     * 1. 课程计划不存在 → 抛出异常
     * 2. 有子节点的大章节 → 不能删除
     * 3. 已关联媒资的小节 → 不能删除
     *
     * @param teachplanId 课程计划ID
     */
    void deleteTeachplan(Long teachplanId);

    /**
     * 课程计划上移
     * <p>
     * 功能说明：
     * - 将当前节点与同级的前一个节点交换位置
     * - 通过交换orderby值实现
     * <p>
     * 业务规则：
     * - 如果已经是第一个，则无法上移
     *
     * @param teachplanId 课程计划ID
     */
    void moveUpTeachplan(Long teachplanId);

    /**
     * 课程计划下移
     * <p>
     * 功能说明：
     * - 将当前节点与同级的后一个节点交换位置
     * - 通过交换orderby值实现
     * <p>
     * 业务规则：
     * - 如果已经是最后一个，则无法下移
     *
     * @param teachplanId 课程计划ID
     */
    void moveDownTeachplan(Long teachplanId);
}
