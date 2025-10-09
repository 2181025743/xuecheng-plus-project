package com.xuecheng.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XuechengPlusException;
import com.xuecheng.content.mapper.TeachplanMapper;
import com.xuecheng.content.mapper.TeachplanMediaMapper;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.model.po.TeachplanMedia;
import com.xuecheng.service.TeachplanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 课程计划管理ServiceImpl
 */
@Slf4j
@Service
public class TeachplanServiceImpl implements TeachplanService {

    @Autowired
    TeachplanMapper teachplanMapper;

    @Autowired
    TeachplanMediaMapper teachplanMediaMapper;

    /**
     * 查询课程计划树形结构
     */
    @Override
    public List<TeachplanDto> findTeachplanTree(Long courseId) {
        return teachplanMapper.selectTreeNodes(courseId);
    }

    /**
     * 新增/修改/保存课程计划
     * <p>
     * 业务逻辑：
     * 1. 通过id判断是新增还是修改
     * - id为null：新增操作（INSERT）
     * - id不为null：修改操作（UPDATE）
     * <p>
     * 2. 新增时需要设置排序号orderby
     * - 查询同级节点的数量
     * - 排序号 = 同级节点数量 + 1
     *
     * @param saveTeachplanDto 课程计划信息
     */
    @Override
    public void saveTeachplan(SaveTeachplanDto saveTeachplanDto) {
        // 通过课程计划id判断是新增还是修改
        Long teachplanId = saveTeachplanDto.getId();

        if (teachplanId == null) {
            // ========== 新增操作 ==========
            log.info("新增课程计划：courseId={}, parentid={}, grade={}, pname={}",
                    saveTeachplanDto.getCourseId(),
                    saveTeachplanDto.getParentid(),
                    saveTeachplanDto.getGrade(),
                    saveTeachplanDto.getPname());

            // 创建Teachplan实体对象
            Teachplan teachplan = new Teachplan();
            // 将DTO属性拷贝到实体对象
            BeanUtils.copyProperties(saveTeachplanDto, teachplan);

            // 确定排序字段：查询同级节点个数，排序字段 = 个数 + 1
            // 同级节点：同一课程(course_id) + 同一父节点(parentid)
            Long parentid = saveTeachplanDto.getParentid();
            Long courseId = saveTeachplanDto.getCourseId();
            int teachplanCount = getTeachplanCount(courseId, parentid);
            teachplan.setOrderby(teachplanCount);

            // 插入数据库
            teachplanMapper.insert(teachplan);

            log.info("新增课程计划成功，ID={}", teachplan.getId());

        } else {
            // ========== 修改操作 ==========
            log.info("修改课程计划：id={}, pname={}", teachplanId, saveTeachplanDto.getPname());

            // 根据id查询课程计划
            Teachplan teachplan = teachplanMapper.selectById(teachplanId);
            if (teachplan == null) {
                log.error("课程计划不存在，id={}", teachplanId);
                return;
            }

            // 将参数复制到teachplan
            BeanUtils.copyProperties(saveTeachplanDto, teachplan);

            // 更新数据库
            teachplanMapper.updateById(teachplan);

            log.info("修改课程计划成功，ID={}", teachplanId);
        }
    }

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
    @Override
    public void deleteTeachplan(Long teachplanId) {
        log.info("删除课程计划，ID={}", teachplanId);

        List<TeachplanDto> teachplan = teachplanMapper.selectTreeNodes(teachplanId);
        if (teachplan == null) {
            log.error("课程计划不存在{}", teachplanId);
            XuechengPlusException.cast("课程计划不存在");
        }
        LambdaQueryWrapper<Teachplan> childWrapper = new LambdaQueryWrapper<>();
        childWrapper.eq(Teachplan::getParentid, teachplanId);
        Integer childCount = teachplanMapper.selectCount(childWrapper);
        if (childCount > 0) {
            log.warn("课程计划下有{}个子节点，无法删除，ID={}", childCount, teachplanId);
            XuechengPlusException.cast(String.format("课程计划下有%d个子节点，无法删除，ID=%d", childCount, teachplanId));
        }
        LambdaQueryWrapper<TeachplanMedia> teachplanMediaLambdaQueryWrapper = new LambdaQueryWrapper<>();
        teachplanMediaLambdaQueryWrapper.eq(TeachplanMedia::getTeachplanId, teachplanId);
        Integer mediaCount = teachplanMediaMapper.selectCount(teachplanMediaLambdaQueryWrapper);
        if (mediaCount > 0) {
            log.warn("课程计划已关联{}个媒资，无法删除，ID={}", mediaCount, teachplanId);
            XuechengPlusException.cast("该课程计划已关联媒资，请先解绑媒资");
        }
        int deleted = teachplanMapper.deleteById(teachplanId);
        if (deleted > 0) {
            log.info("删除课程计划成功，ID={}", teachplanId);
        } else {
            log.error("删除课程计划失败，ID={}", teachplanId);
        }
    }

    /**
     * 获取同级节点的数量
     * <p>
     * 说明：用于确定新增节点的排序号
     *
     * @param courseId 课程ID
     * @param parentId 父节点ID
     * @return 同级节点数量 + 1（作为新节点的排序号）
     */
    private int getTeachplanCount(Long courseId, Long parentId) {
        // 构建查询条件：
        // SELECT COUNT(1) FROM teachplan
        // WHERE course_id = ? AND parentid = ?
        LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Teachplan::getCourseId, courseId)
                .eq(Teachplan::getParentid, parentId);

        // 查询数量
        Integer count = teachplanMapper.selectCount(queryWrapper);

        // 返回数量 + 1（作为新节点的排序号）
        return count + 1;
    }

    @Override
    public void moveDownTeachplan(Long teachplanId) {
        log.info("课程计划下移，ID={}", teachplanId);
        Teachplan currrentNode = teachplanMapper.selectById(teachplanId);
        if (currrentNode == null) {
            log.error("课程计划不存在");
            XuechengPlusException.cast("课程计划不存在");
        }
        LambdaQueryWrapper<Teachplan> teachplanLambdaQueryWrapper = new LambdaQueryWrapper<>();
        Integer currrentNodeOrderby = currrentNode.getOrderby();
        teachplanLambdaQueryWrapper.eq(Teachplan::getCourseId, currrentNode.getCourseId())
                .eq(Teachplan::getParentid, currrentNode.getParentid())
                .gt(Teachplan::getOrderby, currrentNodeOrderby)
                .orderByAsc(Teachplan::getOrderby)
                .last("LiMIT 1");
        Teachplan nextNode = teachplanMapper.selectOne(teachplanLambdaQueryWrapper);
        if (nextNode == null) {
            log.warn("已经是最后一个节点了");
            XuechengPlusException.cast("已经是最后一个节点了");
        }
        Integer nextNodeOrderby = nextNode.getOrderby();
        currrentNode.setOrderby(nextNodeOrderby);
        nextNode.setOrderby(currrentNodeOrderby);
        teachplanMapper.updateById(currrentNode);
        teachplanMapper.updateById(nextNode);
    }

    /**
     * 课程计划上移
     * <p>
     * 业务逻辑：
     * 1. 查询当前节点
     * 2. 查询同级的前一个节点（orderby比当前小的最大值）
     * 3. 交换两个节点的orderby值
     *
     * @param teachplanId 课程计划ID
     */

    @Override
    public void moveUpTeachplan(Long teachplanId) {
        log.info("课程计划上移，ID={}", teachplanId);

        // 步骤1：查询当前节点
        Teachplan currentNode = teachplanMapper.selectById(teachplanId);
        if (currentNode == null) {
            XuechengPlusException.cast("课程计划不存在");
        }

        // 步骤2：查询同级的前一个节点
        // 条件：同一课程 + 同一父节点 + orderby < 当前orderby
        // 排序：按orderby降序，取第一个（即最接近当前节点的前一个）
        LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Teachplan::getCourseId, currentNode.getCourseId())
                .eq(Teachplan::getParentid, currentNode.getParentid())
                .lt(Teachplan::getOrderby, currentNode.getOrderby())
                .orderByDesc(Teachplan::getOrderby)
                .last("LIMIT 1");

        Teachplan previousNode = teachplanMapper.selectOne(queryWrapper);

        if (previousNode == null) {
            log.warn("已经是第一个节点，无法上移，ID={}", teachplanId);
            XuechengPlusException.cast("已经是第一个，无法上移");
        }

        // 步骤3：交换两个节点的orderby值
        Integer currentOrder = currentNode.getOrderby();
        Integer previousOrder = previousNode.getOrderby();

        currentNode.setOrderby(previousOrder);
        previousNode.setOrderby(currentOrder);

        teachplanMapper.updateById(currentNode);
        teachplanMapper.updateById(previousNode);

        log.info("上移成功：当前节点ID={}, orderby: {} → {}",
                teachplanId, currentOrder, previousOrder);
        log.info("上移成功：前一节点ID={}, orderby: {} → {}",
                previousNode.getId(), previousOrder, currentOrder);
    }
}
