package com.xuecheng.content.model.dto;

import lombok.Data;

/**
 * 课程计划保存DTO
 * 
 * 用途：用于新增大章节、新增小章节、修改章节信息
 * 
 * 设计说明：
 * 1. 新增大章节：不传id，parentid=0，grade=1
 * 2. 新增小章节：不传id，parentid=父章节id，grade=2
 * 3. 修改章节：传入id，更新对应的章节信息
 * 
 * @author itcast
 */
@Data
public class SaveTeachplanDto {

    /**
     * 教学计划id
     * 
     * 说明：
     * - 新增时不传（id=null），执行INSERT操作
     * - 修改时必传，执行UPDATE操作
     */
    private Long id;

    /**
     * 课程计划名称
     * 
     * 示例：
     * - 大章节："第1章：Java基础"
     * - 小节："1.1 变量和数据类型"
     */
    private String pname;

    /**
     * 课程计划父级Id
     * 
     * 说明：
     * - 大章节：parentid = 0（表示顶级节点）
     * - 小节：parentid = 父章节的id
     */
    private Long parentid;

    /**
     * 层级，分为1、2、3级
     * 
     * 说明：
     * - grade=1：大章节（一级目录）
     * - grade=2：小节（二级目录）
     * - grade=3：预留（暂不使用）
     */
    private Integer grade;

    /**
     * 课程类型：1视频、2文档
     * 
     * 说明：仅小节才有媒体类型（grade=2时有效）
     */
    private String mediaType;

    /**
     * 课程ID
     * 
     * 说明：该课程计划属于哪门课程（必填）
     */
    private Long courseId;

    /**
     * 课程发布ID
     * 
     * 说明：课程发布后才会有此ID
     */
    private Long coursePubId;

    /**
     * 是否支持试学或预览（试看）
     * 
     * 值：
     * - "1"：支持试看
     * - "0"：不支持试看
     */
    private String isPreview;
}
