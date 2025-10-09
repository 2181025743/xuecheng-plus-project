package com.xuecheng.content.api;

import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import com.xuecheng.service.CourseCategoryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 课程分类管理接口
 * 
 * 提供课程分类的查询功能
 */
@Api(value = "课程分类管理接口", tags = "课程分类管理接口")
@RestController
@RequestMapping("/course-category") // 统一路径前缀
@Slf4j
public class CourseCategoryController {

    @Autowired
    CourseCategoryService courseCategoryService;

    /**
     * 查询课程分类树形结构
     * 
     * 请求方式：GET
     * 请求路径：/course-category/tree-nodes
     * 
     * 返回数据格式：
     * [
     * {
     * "id": "1-1",
     * "name": "前端开发",
     * "childrenTreeNodes": [
     * {"id": "1-1-1", "name": "HTML&CSS", "childrenTreeNodes": []},
     * {"id": "1-1-2", "name": "JavaScript", "childrenTreeNodes": []}
     * ]
     * }
     * ]
     * 
     * @return 课程分类树形列表（从一级分类开始，不包含根节点）
     */
    @ApiOperation("查询课程分类树形结构")
    @GetMapping("/tree-nodes") // 改为相对路径
    public List<CourseCategoryTreeDto> queryTreeNodes() {
        // 查询根节点ID为"1"的所有子分类
        // Service层会将扁平数据组装成树形结构
        return courseCategoryService.queryTreeNodes("1");
    }
}