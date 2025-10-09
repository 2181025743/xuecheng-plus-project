package com.xuecheng.media.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * 分块文件定时清理任务
 * 
 * 🎯 功能说明：
 * 自动清理超过24小时的临时分块文件，防止磁盘空间浪费
 * 
 * 📊 清理策略：
 * 1. 每天凌晨2点自动执行
 * 2. 扫描临时分块目录（/tmp/xc-chunks/ 或 C:\Users\...\Temp\xc-chunks\）
 * 3. 检查每个分块目录的最后修改时间
 * 4. 删除超过24小时的分块目录及其所有内容
 * 
 * 🔍 判断逻辑：
 * - 正常情况下，视频上传在几分钟到几小时内完成
 * - 超过24小时的分块目录，必定是中断或失败的上传
 * - 安全删除，不会影响正在进行的上传
 * 
 * ⚠️ 注意事项：
 * - 时间阈值：24小时（可配置）
 * - 执行时间：每天凌晨2点（用户使用最少的时段）
 * - 删除前会记录详细日志
 * - 删除失败不影响任务继续执行
 * 
 * @author AI Assistant
 * @version 1.0
 * @date 2025-10-07
 */
@Slf4j
@Component
public class ChunkCleanupTask {

    /**
     * 分块文件根目录
     */
    private static final String CHUNK_ROOT = System.getProperty("java.io.tmpdir") + "/xc-chunks";

    /**
     * 清理阈值（小时）
     */
    private static final int CLEANUP_THRESHOLD_HOURS = 24;

    /**
     * 定时清理任务
     * 
     * cron表达式：0 0 2 * * ?
     * - 秒：0
     * - 分：0
     * - 时：2（凌晨2点）
     * - 日：*（每天）
     * - 月：*（每月）
     * - 星期：?（不指定）
     * 
     * 执行时间：每天凌晨2点执行一次
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOldChunks() {
        log.info("========== 开始执行分块文件清理任务 ==========");

        Path rootPath = Paths.get(CHUNK_ROOT);

        // 检查根目录是否存在
        if (!Files.exists(rootPath)) {
            log.info("分块根目录不存在，无需清理：{}", CHUNK_ROOT);
            return;
        }

        int totalDirs = 0; // 总目录数
        int deletedDirs = 0; // 删除的目录数
        long freedSpace = 0; // 释放的空间（字节）

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(rootPath)) {
            for (Path chunkDir : stream) {
                // 只处理目录
                if (!Files.isDirectory(chunkDir)) {
                    continue;
                }

                totalDirs++;

                try {
                    // 获取目录的最后修改时间
                    BasicFileAttributes attrs = Files.readAttributes(chunkDir, BasicFileAttributes.class);
                    Instant lastModified = attrs.lastModifiedTime().toInstant();
                    Instant threshold = Instant.now().minus(CLEANUP_THRESHOLD_HOURS, ChronoUnit.HOURS);

                    // 判断是否超过阈值
                    if (lastModified.isBefore(threshold)) {
                        // 计算目录大小
                        long dirSize = calculateDirSize(chunkDir);

                        // 删除目录及其所有内容
                        deleteDirectory(chunkDir);

                        deletedDirs++;
                        freedSpace += dirSize;

                        log.info("✅ 清理过期分块目录：{}，大小：{}MB，最后修改时间：{}",
                                chunkDir.getFileName(),
                                dirSize / 1024 / 1024,
                                lastModified);
                    } else {
                        log.debug("分块目录未过期，跳过：{}，最后修改时间：{}",
                                chunkDir.getFileName(), lastModified);
                    }

                } catch (Exception e) {
                    log.error("处理分块目录失败：{}，错误：{}", chunkDir, e.getMessage(), e);
                    // 继续处理下一个目录
                }
            }
        } catch (IOException e) {
            log.error("扫描分块根目录失败：{}", e.getMessage(), e);
        }

        log.info("========== 分块文件清理任务完成 ==========");
        log.info("扫描目录数：{}，清理目录数：{}，释放空间：{}MB",
                totalDirs, deletedDirs, freedSpace / 1024 / 1024);
    }

    /**
     * 计算目录大小（递归计算所有文件）
     * 
     * @param dir 目录路径
     * @return 目录总大小（字节）
     */
    private long calculateDirSize(Path dir) {
        long size = 0;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path file : stream) {
                if (Files.isRegularFile(file)) {
                    size += Files.size(file);
                } else if (Files.isDirectory(file)) {
                    // 递归计算子目录大小
                    size += calculateDirSize(file);
                }
            }
        } catch (IOException e) {
            log.warn("计算目录大小失败：{}", dir, e);
        }
        return size;
    }

    /**
     * 递归删除目录及其所有内容
     * 
     * @param dir 要删除的目录
     * @throws IOException 删除失败时抛出异常
     */
    private void deleteDirectory(Path dir) throws IOException {
        // 先删除目录中的所有文件
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path file : stream) {
                if (Files.isDirectory(file)) {
                    // 递归删除子目录
                    deleteDirectory(file);
                } else {
                    // 删除文件
                    Files.deleteIfExists(file);
                }
            }
        }
        // 最后删除空目录
        Files.deleteIfExists(dir);
    }

    /**
     * 手动触发清理（用于测试）
     * 
     * 可以通过HTTP接口或管理工具调用此方法进行手动清理
     */
    public void manualCleanup() {
        log.info("手动触发分块文件清理任务");
        cleanupOldChunks();
    }

    /**
     * 获取分块文件统计信息（用于监控）
     * 
     * @return 统计信息字符串
     */
    public String getStatistics() {
        Path rootPath = Paths.get(CHUNK_ROOT);

        if (!Files.exists(rootPath)) {
            return "分块根目录不存在";
        }

        int dirCount = 0;
        long totalSize = 0;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(rootPath)) {
            for (Path dir : stream) {
                if (Files.isDirectory(dir)) {
                    dirCount++;
                    totalSize += calculateDirSize(dir);
                }
            }
        } catch (IOException e) {
            log.error("统计分块文件失败", e);
            return "统计失败：" + e.getMessage();
        }

        return String.format("分块目录数：%d，总大小：%dMB", dirCount, totalSize / 1024 / 1024);
    }
}
