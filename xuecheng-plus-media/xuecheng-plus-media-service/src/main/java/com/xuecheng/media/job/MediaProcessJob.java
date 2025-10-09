package com.xuecheng.media.job;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xuecheng.media.task.ChunkCleanupTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 媒资处理任务（XXL-JOB）
 * 
 * @author xuecheng
 */
@Slf4j
@Component
public class MediaProcessJob {

    @Autowired
    private ChunkCleanupTask chunkCleanupTask;

    /**
     * 测试任务
     * JobHandler: testJob
     * Cron: 0/10 * * * * ? (每10秒执行一次)
     */
    @XxlJob("testJob")
    public void testJob() {
        log.info("========== 测试任务开始执行 ==========");
        log.info("执行时间：{}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        log.info("任务参数：{}", XxlJobHelper.getJobParam());

        try {
            // 模拟任务执行
            Thread.sleep(2000);
            log.info("任务执行中...");

            // 返回执行结果
            XxlJobHelper.handleSuccess("任务执行成功！");
        } catch (Exception e) {
            log.error("任务执行失败", e);
            XxlJobHelper.handleFail("任务执行失败：" + e.getMessage());
        }

        log.info("========== 测试任务执行完成 ==========");
    }

    /**
     * 分块文件清理任务（整合现有功能）
     * JobHandler: chunkCleanupJob
     * Cron: 0 0 2 * * ? (每天凌晨2点执行)
     */
    @XxlJob("chunkCleanupJob")
    public void chunkCleanupJob() {
        log.info("========== 分块文件清理任务开始 ==========");
        log.info("执行时间：{}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        try {
            // 调用现有的清理任务
            chunkCleanupTask.cleanupOldChunks();

            // 获取统计信息
            String statistics = chunkCleanupTask.getStatistics();
            log.info("清理统计：{}", statistics);

            XxlJobHelper.handleSuccess("分块文件清理成功！" + statistics);
        } catch (Exception e) {
            log.error("分块文件清理失败", e);
            XxlJobHelper.handleFail("分块文件清理失败：" + e.getMessage());
        }

        log.info("========== 分块文件清理任务完成 ==========");
    }

    /**
     * 视频转码任务（待实现）
     * JobHandler: videoTranscodeJob
     * Cron: 0 0/5 * * * ? (每5分钟执行一次)
     */
    @XxlJob("videoTranscodeJob")
    public void videoTranscodeJob() {
        log.info("========== 视频转码任务开始 ==========");
        log.info("执行时间：{}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        // 获取分片参数（支持分布式执行）
        int shardIndex = XxlJobHelper.getShardIndex(); // 当前分片索引（从0开始）
        int shardTotal = XxlJobHelper.getShardTotal(); // 总分片数

        log.info("分片参数：当前分片 = {}/{}", shardIndex + 1, shardTotal);

        try {
            // TODO: 实现视频转码逻辑
            // 1. 查询待转码视频（根据分片参数筛选）
            // 2. 调用FFmpeg转码
            // 3. 更新视频状态

            log.info("TODO: 查询待转码视频（分片索引: {}）", shardIndex);
            log.info("TODO: 调用FFmpeg转码工具");
            log.info("TODO: 更新视频转码状态");

            XxlJobHelper.handleSuccess("视频转码任务执行成功（待实现）");
        } catch (Exception e) {
            log.error("视频转码任务失败", e);
            XxlJobHelper.handleFail("视频转码任务失败：" + e.getMessage());
        }

        log.info("========== 视频转码任务完成 ==========");
    }

    /**
     * 分片广播测试任务（视频教学演示）
     * JobHandler: shardingJobHandler
     * Cron: 0/5 * * * * ? (每5秒执行一次)
     * 
     * 路由策略：分片广播（SHARDING_BROADCAST）
     * 
     * 功能说明：
     * 1. 测试分片广播机制是否正常工作
     * 2. 打印当前执行器的分片参数
     * 3. 验证多个执行器是否同时执行任务
     */
    @XxlJob("shardingJobHandler")
    public void shardingJobHandler() {
        // 获取分片参数
        int shardIndex = XxlJobHelper.getShardIndex(); // 当前执行器序号（从0开始）
        int shardTotal = XxlJobHelper.getShardTotal(); // 执行器总数

        log.info("========== 分片广播任务执行 ==========");
        log.info("分片参数 → 当前分片序号: {}, 总分片数: {}", shardIndex, shardTotal);
        log.info("执行时间：{}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        try {
            // 模拟业务逻辑：每个执行器处理自己的分片数据
            log.info("【执行器 {}】开始处理属于自己的数据分片...", shardIndex);
            
            // 示例：如何根据分片参数处理数据
            // SELECT * FROM media_process 
            // WHERE status = '待处理' 
            // AND MOD(id, #{shardTotal}) = #{shardIndex}
            // LIMIT 100
            
            log.info("【执行器 {}】处理逻辑：查询 id % {} = {} 的数据", shardIndex, shardTotal, shardIndex);
            
            // 模拟任务执行耗时
            Thread.sleep(1000);
            
            log.info("【执行器 {}】数据处理完成！", shardIndex);
            
            XxlJobHelper.handleSuccess("分片任务执行成功 - 分片序号: " + shardIndex);
        } catch (Exception e) {
            log.error("分片任务执行失败", e);
            XxlJobHelper.handleFail("分片任务执行失败：" + e.getMessage());
        }
        
        log.info("========== 分片广播任务完成 ==========\n");
    }

    /**
     * 示例：带参数的任务
     * JobHandler: paramJob
     * 演示如何接收和使用任务参数
     */
    @XxlJob("paramJob")
    public void paramJob() {
        log.info("========== 参数任务开始执行 ==========");

        // 获取任务参数（在调度中心配置）
        String param = XxlJobHelper.getJobParam();
        log.info("接收到的任务参数：{}", param);

        try {
            // 根据参数执行不同逻辑
            if (param != null && !param.isEmpty()) {
                log.info("根据参数 [{}] 执行特定逻辑", param);
            }

            XxlJobHelper.handleSuccess("参数任务执行成功，参数：" + param);
        } catch (Exception e) {
            log.error("参数任务执行失败", e);
            XxlJobHelper.handleFail(e.getMessage());
        }

        log.info("========== 参数任务执行完成 ==========");
    }
}
