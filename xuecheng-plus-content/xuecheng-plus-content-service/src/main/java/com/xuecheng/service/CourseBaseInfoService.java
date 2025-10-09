package com.xuecheng.service;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;

public interface CourseBaseInfoService {
    PageResult<CourseBase> queryCourseBaseList(PageParams pageParams, QueryCourseParamsDto queryCourseParamsDto);

    /**
     * 新增课程
     *
     * @param companyId    机构ID（从登录用户获取，当前可以写死测试）
     * @param addCourseDto 课程信息（基本信息+营销信息）
     * @return 添加成功后的课程完整信息
     */
    CourseBaseInfoDto createCourse(Long companyId, AddCourseDto addCourseDto);

    /**
     * 修改课程
     *
     * @param companyId    机构ID（从登录用户获取）
     * @param addCourseDto 课程信息（必须包含课程ID）
     * @return 修改成功后的课程完整信息
     */
    CourseBaseInfoDto updateCourse(Long companyId, AddCourseDto addCourseDto);

    /**
     * 根据课程ID查询课程详情
     *
     * @param courseId 课程ID
     * @return 课程完整信息
     */
    CourseBaseInfoDto getCourseBaseInfo(Long courseId);
}
