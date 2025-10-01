package com.xuecheng.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.service.CourseBaseInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
public class CourseBaseInfoServiceImpl implements CourseBaseInfoService {

    @Autowired
    CourseBaseMapper courseBaseMapper;

    @Override
    public PageResult<CourseBase> queryCourseBaseList(PageParams pageParams,
            QueryCourseParamsDto queryCourseParamsDto) {
        // 2. 构建查询条件
        log.info("2. 构建分页查询条件");
        LambdaQueryWrapper<CourseBase> queryWrapper = new LambdaQueryWrapper<>();

        // 课程名称模糊查询
        queryWrapper.like(!StringUtils.isEmpty(queryCourseParamsDto.getCourseName()),
                CourseBase::getName,
                queryCourseParamsDto.getCourseName());

        // 课程审核状态精确查询
        queryWrapper.eq(!StringUtils.isEmpty(queryCourseParamsDto.getAuditStatus()),
                CourseBase::getAuditStatus,
                queryCourseParamsDto.getAuditStatus());

        // 课程发布状态精确查询
        queryWrapper.eq(!StringUtils.isEmpty(queryCourseParamsDto.getPublishStatus()),
                CourseBase::getStatus,
                queryCourseParamsDto.getPublishStatus());

        // 3. 设置分页参数
        log.info("分页参数：第{}页，每页{}条", pageParams.getPageNo(), pageParams.getPageSize());
        log.info("查询条件：课程名称={}，审核状态={}，发布状态={}",
                queryCourseParamsDto.getCourseName(),
                queryCourseParamsDto.getAuditStatus(),
                queryCourseParamsDto.getPublishStatus());

        // 4. 执行分页查询
        log.info("3. 执行分页查询...");
        Page<CourseBase> courseBasePage = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
        Page<CourseBase> result = courseBaseMapper.selectPage(courseBasePage, queryWrapper);

        // 5. 封装结果
        List<CourseBase> items = result.getRecords();
        long total = result.getTotal();
        PageResult<CourseBase> courseBasePageResult = new PageResult<>(
                items, total, pageParams.getPageNo(), pageParams.getPageSize());

        // 6. 验证结果
        log.info("========== 查询结果 ==========");
        log.info("✅ 总记录数：{}", total);
        log.info("✅ 当前页码：{}", courseBasePageResult.getPage());
        log.info("✅ 每页大小：{}", courseBasePageResult.getPageSize());
        log.info("✅ 本页数据：{} 条", items.size());

        items.forEach(item -> {
            log.info("  - 课程ID：{}，课程名称：{}，审核状态：{}，发布状态：{}",
                    item.getId(), item.getName(), item.getAuditStatus(), item.getStatus());
        });

        log.info("========== 测试完成 ==========\n");

        return courseBasePageResult;
    }
}
