package com.xuecheng.content.api;

import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.service.TeachplanService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 课程计划管理接口
 * <p>
 * 功能：
 * - 查询课程计划树形结构
 * - 添加课程计划（章/节）
 * - 修改课程计划
 * - 删除课程计划
 * - 课程计划排序
 */
@Api(value = "课程计划编辑接口", tags = "课程计划编辑接口")
@RestController
public class TeachplanController {

    @Autowired
    TeachplanService teachplanService;

    /**
     * 查询课程计划树形结构
     * <p>
     * 请求示例：GET /teachplan/22/tree-nodes
     * <p>
     * 返回数据格式：
     * [
     * {
     * "id": 1,
     * "pname": "第1章：Java基础",
     * "grade": 1,
     * "teachplanMedia": null,
     * "teachPlanTreeNodes": [
     * {
     * "id": 101,
     * "pname": "1.1 变量和数据类型",
     * "grade": 2,
     * "teachplanMedia": {
     * "mediaId": "video001",
     * "mediaFilename": "变量.mp4"
     * },
     * "teachPlanTreeNodes": []
     * }
     * ]
     * }
     * ]
     *
     * @param courseId 课程ID
     * @return 课程计划树形列表
     */
    @ApiOperation("查询课程计划树形结构")
    @ApiImplicitParam(name = "courseId", value = "课程ID", required = true, dataType = "Long", paramType = "path")
    @GetMapping("/teachplan/{courseId}/tree-nodes")
    public List<TeachplanDto> getTreeNodes(@PathVariable Long courseId) {
        return teachplanService.findTeachplanTree(courseId);
    }

    /**
     * 课程计划创建或修改
     * <p>
     * 接口说明：一个接口实现三个功能
     * 1. 添加大章节：不传id，parentid=0，grade=1
     * 2. 添加小章节：不传id，parentid=父章节id，grade=2
     * 3. 修改章节：传入id，更新对应的章节信息
     * <p>
     * 请求示例：POST /teachplan
     * <p>
     * 请求体（添加大章节）：
     * {
     * "courseId": 117,
     * "parentid": 0,
     * "grade": 1,
     * "pname": "新章名称 [点击修改]"
     * }
     * <p>
     * 请求体（添加小章节）：
     * {
     * "courseId": 117,
     * "parentid": 255,
     * "grade": 2,
     * "pname": "新小节名称 [点击修改]"
     * }
     * <p>
     * 请求体（修改章节）：
     * {
     * "id": 255,
     * "courseId": 117,
     * "parentid": 0,
     * "grade": 1,
     * "pname": "配置管理",
     * "orderby": 1
     * }
     *
     * @param saveTeachplanDto 课程计划信息
     */
    @ApiOperation("课程计划创建或修改")
    @PostMapping("/teachplan")
    public void saveTeachplan(@RequestBody SaveTeachplanDto saveTeachplanDto) {
        teachplanService.saveTeachplan(saveTeachplanDto);
    }

    /**
     * 删除课程计划
     * <p>
     * 接口说明：
     * - URL: DELETE /teachplan/{teachplanId}
     * - 功能：删除指定的课程计划（大章节或小节）
     * - 业务规则：
     * 1. 不能删除有子节点的大章节
     * 2. 不能删除已关联媒资的小节
     * <p>
     * 请求示例：DELETE /teachplan/269
     *
     * @param teachplanId 课程计划ID
     */
    @ApiOperation("删除课程计划")
    @DeleteMapping("/teachplan/{teachplanId}")
    public void deleteTeachplan(@PathVariable Long teachplanId) {
        teachplanService.deleteTeachplan(teachplanId);
    }

    /**
     * 课程计划上移
     * <p>
     * 接口说明：
     * - URL: POST /teachplan/moveup/{teachplanId}
     * - 功能：将课程计划向上移动一位
     * - 逻辑：与同级的前一个节点交换orderby值
     * <p>
     * 请求示例：POST /teachplan/moveup/256
     *
     * @param teachplanId 课程计划ID
     */
    @ApiOperation("课程计划上移")
    @PostMapping("/teachplan/moveup/{teachplanId}")
    public void moveUpTeachplan(@PathVariable Long teachplanId) {
        teachplanService.moveUpTeachplan(teachplanId);
    }

    /**
     * 课程计划下移
     * <p>
     * 接口说明：
     * - URL: POST /teachplan/movedown/{teachplanId}
     * - 功能：将课程计划向下移动一位
     * - 逻辑：与同级的后一个节点交换orderby值
     * <p>
     * 请求示例：POST /teachplan/movedown/256
     *
     * @param teachplanId 课程计划ID
     */
    @ApiOperation("课程计划下移")
    @PostMapping("/teachplan/movedown/{teachplanId}")
    public void moveDownTeachplan(@PathVariable Long teachplanId) {
        teachplanService.moveDownTeachplan(teachplanId);
    }
}
