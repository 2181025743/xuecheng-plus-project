package com.xuecheng.base.exception;

/**
 * 校验分组
 * 用于JSR 303分组校验，区分不同接口的校验规则
 * 
 * 使用场景：
 * - 同一个DTO类被多个接口使用
 * - 不同接口对字段的校验要求不同
 * - 通过分组来区分校验规则
 * 
 * 示例：
 * - 新增课程：课程ID不需要（使用Insert组）
 * - 修改课程：课程ID必须（使用Update组）
 */
public class ValidationGroups {

    /**
     * 新增操作的校验组
     */
    public interface Insert {
    }

    /**
     * 修改操作的校验组
     */
    public interface Update {
    }

    /**
     * 删除操作的校验组
     */
    public interface Delete {
    }
}
