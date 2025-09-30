package com.xuecheng.content;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@SpringBootTest
public class CourseBaseMapperTest {
    @Autowired
    CourseBaseMapper courseBaseMapper;

    @Test
    public void testCourseBaseMapper() {
        log.info("========== 开始测试课程分页查询 ==========");

        // 1. 测试根据ID查询
        log.info("1. 测试根据ID查询课程");
        CourseBase courseBase = courseBaseMapper.selectById(18);
        Assertions.assertNotNull(courseBase);
        log.info("✅ 查询到课程：{}", courseBase.getName());

        // 2. 构建查询条件
        log.info("2. 构建分页查询条件");
        LambdaQueryWrapper<CourseBase> queryWrapper = new LambdaQueryWrapper<>();
        QueryCourseParamsDto courseParamsDto = new QueryCourseParamsDto();
        courseParamsDto.setCourseName("java");

        queryWrapper.like(!StringUtils.isEmpty(courseParamsDto.getCourseName()),
                CourseBase::getName,
                courseParamsDto.getCourseName());
        queryWrapper.eq(!StringUtils.isEmpty(courseParamsDto.getAuditStatus()),
                CourseBase::getAuditStatus,
                courseBase.getAuditStatus());

        // 3. 设置分页参数
        PageParams pageParams = new PageParams(1L, 3L);
        log.info("分页参数：第{}页，每页{}条", pageParams.getPageNo(), pageParams.getPageSize());

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
            log.info("  - 课程ID：{}，课程名称：{}", item.getId(), item.getName());
        });

        log.info("========== 测试完成 ==========\n");

        // 断言验证
        Assertions.assertNotNull(courseBasePageResult);
        Assertions.assertTrue(total > 0, "应该查询到数据");
    }
}