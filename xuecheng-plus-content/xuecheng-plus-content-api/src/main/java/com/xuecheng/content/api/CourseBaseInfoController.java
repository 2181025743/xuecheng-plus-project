package com.xuecheng.content.api;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.service.CourseBaseInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
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

}
