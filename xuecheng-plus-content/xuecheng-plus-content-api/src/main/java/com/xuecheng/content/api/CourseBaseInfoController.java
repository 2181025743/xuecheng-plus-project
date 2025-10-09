package com.xuecheng.content.api;

import com.xuecheng.base.exception.ValidationGroups;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.service.CourseBaseInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Api(value = "课程信息编辑接口", tags = "课程信息编辑接口")
@RestController
@RequestMapping("/course")
public class CourseBaseInfoController {
    @Autowired
    CourseBaseInfoService courseBaseInfoService;

    @ApiOperation("课程查询接口")
    @PostMapping("/list")
    public PageResult<CourseBase> list(
            @RequestParam(value = "pageNo", defaultValue = "1") Long pageNo,
            @RequestParam(value = "pageSize", defaultValue = "10") Long pageSize,
            @RequestBody(required = false) QueryCourseParamsDto queryCourseParamsDto) {

        // 构建分页参数
        PageParams pageParams = new PageParams(pageNo, pageSize);

        // 如果查询条件为空，创建一个空对象
        if (queryCourseParamsDto == null) {
            queryCourseParamsDto = new QueryCourseParamsDto();
        }

        return courseBaseInfoService.queryCourseBaseList(pageParams,
                queryCourseParamsDto);
    }

    /**
     * 新增课程
     *
     * @param addCourseDto 课程信息
     *                     使用@Validated(ValidationGroups.Insert.class)激活JSR 303校验
     *                     只会校验groups包含Insert.class的注解
     * @return 课程完整信息
     */
    @ApiOperation("新增课程")
    @PostMapping
    public CourseBaseInfoDto createCourse(
            @Validated(ValidationGroups.Insert.class) @RequestBody AddCourseDto addCourseDto) {

        // 机构ID：从登录用户Session中获取（暂时写死测试）
        // TODO: 集成Spring Security后，从SecurityContextHolder获取
        Long companyId = 1232141425L; // 暂时写死的机构ID

        // 调用Service层创建课程
        // 注意：Service层的校验可以保留（防御性编程）
        // 或者注释掉（因为Controller层已经用JSR 303校验了）
        return courseBaseInfoService.createCourse(companyId, addCourseDto);
    }

    /**
     * 根据课程ID查询课程详情
     * 用于编辑课程时的数据回显
     *
     * @param courseId 课程ID
     * @return 课程完整信息
     */
    @ApiOperation("根据ID查询课程")
    @GetMapping("/{courseId}")
    public CourseBaseInfoDto getCourseBaseInfo(@PathVariable Long courseId) {
        return courseBaseInfoService.getCourseBaseInfo(courseId);
    }

    /**
     * 修改课程
     *
     * @param addCourseDto 课程信息（包含ID）
     *                     使用@Validated(ValidationGroups.Update.class)激活JSR 303校验
     *                     只会校验groups包含Update.class的注解
     * @return 课程完整信息
     */
    @ApiOperation("修改课程")
    @PutMapping
    public CourseBaseInfoDto updateCourse(
            @Validated(ValidationGroups.Update.class) @RequestBody AddCourseDto addCourseDto) {

        // 机构ID：从登录用户Session中获取（暂时写死测试）
        // TODO: 集成Spring Security后，从SecurityContextHolder获取
        Long companyId = 1232141425L;

        // 调用Service层修改课程
        return courseBaseInfoService.updateCourse(companyId, addCourseDto);
    }
}
