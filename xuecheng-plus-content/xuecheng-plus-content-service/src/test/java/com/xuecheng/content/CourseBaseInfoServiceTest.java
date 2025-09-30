package com.xuecheng.content;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.service.CourseBaseInfoService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
public class CourseBaseInfoServiceTest {

    @Autowired
    CourseBaseInfoService courseBaseInfoService;

    /**
     * 测试1：按课程名称模糊查询（第1页）
     */
    @Test
    public void testQueryByCourseName() {
        log.info("========== 测试1：按课程名称模糊查询 ==========");

        // 准备查询条件
        QueryCourseParamsDto queryCourseParamsDto = new QueryCourseParamsDto();
        queryCourseParamsDto.setCourseName("java");

        // 准备分页参数
        PageParams pageParams = new PageParams(1L, 3L);

        // 执行查询
        PageResult<CourseBase> result = courseBaseInfoService.queryCourseBaseList(
                pageParams,
                queryCourseParamsDto);

        // 验证结果
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.getCounts() > 0, "应该查询到包含'java'的课程");

        // 计算总页数
        long totalPages = (result.getCounts() + pageParams.getPageSize() - 1) / pageParams.getPageSize();

        log.info("✅ 测试通过：查询到 {} 条记录，共 {} 页", result.getCounts(), totalPages);
        result.getItems().forEach(course -> {
            log.info("  课程ID: {}, 课程名称: {}, 审核状态: {}",
                    course.getId(), course.getName(), course.getAuditStatus());
        });
    }

    /**
     * 测试2：按课程名称查询（第2页）
     */
    @Test
    public void testQueryPage2() {
        log.info("========== 测试2：按课程名称查询第2页 ==========");

        QueryCourseParamsDto queryCourseParamsDto = new QueryCourseParamsDto();
        queryCourseParamsDto.setCourseName("java");

        // 查询第2页
        PageParams pageParams = new PageParams(2L, 3L);

        PageResult<CourseBase> result = courseBaseInfoService.queryCourseBaseList(
                pageParams,
                queryCourseParamsDto);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(2L, result.getPage(), "应该返回第2页数据");

        // 计算总页数
        long totalPages = (result.getCounts() + pageParams.getPageSize() - 1) / pageParams.getPageSize();

        log.info("✅ 测试通过：第{}页，本页{}条数据，共{}页", result.getPage(), result.getItems().size(), totalPages);
    }

    /**
     * 按审核状态查询课程列表
     * <p>
     * 该测试方法验证根据审核状态查询课程的功能。它设置查询条件，包括课程名称和审核状态，
     * 然后调用服务方法执行查询，并验证返回结果的正确性。
     * </p>
     *
     * @see CourseBaseInfoService#queryCourseBaseList(PageParams, QueryCourseParamsDto)
     * @see QueryCourseParamsDto#setAuditStatus(String)
     * @see PageResult#getItems()
     */
    @Test
    public void testQueryByAuditStatus() {
        log.info("========== 测试3：按审核状态查询 ==========");

        // 构造查询条件
        QueryCourseParamsDto queryCourseParamsDto = new QueryCourseParamsDto();
        queryCourseParamsDto.setCourseName("java");
        queryCourseParamsDto.setAuditStatus("202004"); // 审核通过

        PageParams pageParams = new PageParams(1L, 10L);

        // 执行查询
        PageResult<CourseBase> result = courseBaseInfoService.queryCourseBaseList(
                pageParams,
                queryCourseParamsDto);

        Assertions.assertNotNull(result);

        // 计算总页数
        long totalPages = (result.getCounts() + pageParams.getPageSize() - 1) / pageParams.getPageSize();

        // 验证所有返回的课程审核状态都是202004
        result.getItems().forEach(item -> {
            Assertions.assertEquals("202004", item.getAuditStatus(),
                    "课程[" + item.getName() + "]的审核状态应该是202004");
            log.info("  ✓ 课程ID：{}，名称：{}，审核状态：{}",
                    item.getId(), item.getName(), item.getAuditStatus());
        });

        log.info("✅ 测试通过：查询到 {} 条审核通过的课程，共 {} 页", result.getCounts(), totalPages);
    }

    /**
     * 测试4：组合查询（课程名称 + 审核状态）
     */
    @Test
    public void testQueryByCombinedConditions() {
        log.info("========== 测试4：组合查询（名称+审核状态） ==========");

        QueryCourseParamsDto queryCourseParamsDto = new QueryCourseParamsDto();
        queryCourseParamsDto.setCourseName("java");
        queryCourseParamsDto.setAuditStatus("202004");

        PageParams pageParams = new PageParams(1L, 5L);

        PageResult<CourseBase> result = courseBaseInfoService.queryCourseBaseList(
                pageParams,
                queryCourseParamsDto);

        Assertions.assertNotNull(result);

        // 计算总页数
        long totalPages = (result.getCounts() + pageParams.getPageSize() - 1) / pageParams.getPageSize();

        log.info("✅ 测试通过：组合查询到 {} 条记录，共 {} 页", result.getCounts(), totalPages);
    }

    /**
     * 测试5：查询所有课程（无条件）
     */
    @Test
    public void testQueryAll() {
        log.info("========== 测试5：查询所有课程（无条件） ==========");

        QueryCourseParamsDto queryCourseParamsDto = new QueryCourseParamsDto();
        // 不设置任何查询条件

        PageParams pageParams = new PageParams(1L, 10L);

        PageResult<CourseBase> result = courseBaseInfoService.queryCourseBaseList(
                pageParams,
                queryCourseParamsDto);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.getCounts() > 0, "数据库应该有课程数据");

        // 计算总页数
        long totalPages = (result.getCounts() + pageParams.getPageSize() - 1) / pageParams.getPageSize();

        log.info("✅ 测试通过：数据库共有 {} 条课程，共 {} 页", result.getCounts(), totalPages);
    }

    /**
     * 测试6：测试不同的审核状态
     */
    @Test
    public void testQueryByDifferentAuditStatus() {
        log.info("========== 测试6：测试不同的审核状态 ==========");

        // 测试各种审核状态
        String[] auditStatuses = {"202001", "202002", "202003", "202004"};
        String[] auditNames = {"审核未通过", "未提交", "已提交", "审核通过"};

        for (int i = 0; i < auditStatuses.length; i++) {
            QueryCourseParamsDto dto = new QueryCourseParamsDto();
            dto.setAuditStatus(auditStatuses[i]);

            PageParams pageParams = new PageParams(1L, 100L);

            PageResult<CourseBase> result = courseBaseInfoService.queryCourseBaseList(
                    pageParams,
                    dto);

            // 计算总页数
            long totalPages = (result.getCounts() + pageParams.getPageSize() - 1) / pageParams.getPageSize();

            log.info("  状态[{}] - {} 的课程数量：{} 条，共 {} 页",
                    auditStatuses[i], auditNames[i], result.getCounts(), totalPages);
        }

        log.info("✅ 测试完成：已测试所有审核状态");
    }

    /**
     * 测试7：测试分页边界（最后一页）
     */
    @Test
    public void testQueryLastPage() {
        log.info("========== 测试7：测试分页边界（最后一页） ==========");

        QueryCourseParamsDto queryCourseParamsDto = new QueryCourseParamsDto();
        queryCourseParamsDto.setCourseName("java");

        // 先查询总数
        PageParams pageParams1 = new PageParams(1L, 3L);
        PageResult<CourseBase> result1 = courseBaseInfoService.queryCourseBaseList(
                pageParams1,
                queryCourseParamsDto);

        long total = result1.getCounts();
        long lastPage = (total + 2) / 3; // 计算最后一页

        // 查询最后一页
        PageParams pageParams2 = new PageParams(lastPage, 3L);
        PageResult<CourseBase> result2 = courseBaseInfoService.queryCourseBaseList(
                pageParams2,
                queryCourseParamsDto);

        Assertions.assertNotNull(result2);
        Assertions.assertTrue(result2.getItems().size() <= 3, "最后一页数据不应该超过每页大小");

        // 计算总页数
        long totalPages = (total + pageParams1.getPageSize() - 1) / pageParams1.getPageSize();

        log.info("✅ 测试通过：总{}条，共{}页，最后一页（第{}页）有{}条数据",
                total, totalPages, lastPage, result2.getItems().size());
    }
}