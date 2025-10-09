package com.xuecheng.media.api;

import com.xuecheng.media.task.ChunkCleanupTask;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 分块文件管理接口
 * 
 * 🎯 功能说明：
 * 提供分块文件的管理和监控功能
 * 
 * 📊 接口列表：
 * 1. 查询分块文件统计信息
 * 2. 手动触发清理任务
 * 
 * ⚠️ 注意：
 * 此接口仅供管理员使用，生产环境应添加权限控制
 * 
 * @author AI Assistant
 * @version 1.0
 * @date 2025-10-07
 */
@Slf4j
@Api(value = "分块文件管理接口", tags = "分块文件管理")
@RestController
@RequestMapping("/admin/chunk")
public class ChunkManagementController {

    @Autowired
    private ChunkCleanupTask cleanupTask;

    /**
     * 查询当前分块文件统计信息
     * 
     * 返回：分块目录数量和总大小
     * 
     * 访问示例：
     * GET http://localhost:63050/media/admin/chunk/statistics
     */
    @ApiOperation("查询分块文件统计")
    @GetMapping("/statistics")
    public String getStatistics() {
        log.info("查询分块文件统计信息");
        String stats = cleanupTask.getStatistics();
        log.info("统计结果：{}", stats);
        return stats;
    }

    /**
     * 手动触发清理任务
     * 
     * 说明：
     * - 立即执行清理任务，删除超过24小时的分块文件
     * - 用于测试或紧急清理
     * - 正常情况下由定时任务自动执行（每天凌晨2点）
     * 
     * 访问示例：
     * POST http://localhost:63050/media/admin/chunk/cleanup
     */
    @ApiOperation("手动触发清理")
    @PostMapping("/cleanup")
    public String manualCleanup() {
        log.info("收到手动清理请求");
        cleanupTask.manualCleanup();
        return "清理任务已执行，请查看日志了解详情";
    }
}
