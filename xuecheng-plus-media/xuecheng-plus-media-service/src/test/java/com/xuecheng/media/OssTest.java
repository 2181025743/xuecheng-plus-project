package com.xuecheng.media;

import com.xuecheng.media.service.impl.OssService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * 阿里云OSS功能测试类
 * <p>
 * 测试目录：D:\project\OSStest\
 * 文件格式：PNG图片
 * <p>
 * 功能测试：
 * 1. 文件上传（单个）
 * 2. 批量上传
 * 3. 文件下载
 * 4. 文件删除
 * 5. 文件列表查询
 * 6. 生成访问URL
 * 7. MD5完整性校验
 *
 * @author 学成在线项目组
 */
@SpringBootTest
public class OssTest {

    /**
     * 测试目录
     */
    private static final String TEST_DIR = "D:/project/OSStest/";
    @Autowired
    private OssService ossService;

    /**
     * 测试1：上传单个文件
     * <p>
     * 测试场景：
     * 1. 上传PNG图片到OSS
     * 2. 文件按日期自动分目录（如：2025/01/06/文件名.png）
     * 3. 返回OSS中的文件路径
     */
    @Test
    public void test01_uploadFile() {
        // 1. 选择测试文件
        String localFilePath = TEST_DIR + "test.png";

        // 2. 上传文件
        String objectName = ossService.uploadFile(localFilePath);

        // 3. 打印结果
        System.out.println("======================================");
        System.out.println("文件上传成功！");
        System.out.println("本地路径：" + localFilePath);
        System.out.println("OSS路径：" + objectName);
        System.out.println("访问URL：" + ossService.getFileUrl(objectName));
        System.out.println("======================================");
    }

    /**
     * 测试2：批量上传文件
     * <p>
     * 测试场景：
     * 上传 D:\project\OSStest\ 目录下的所有PNG文件
     */
    @Test
    public void test02_batchUpload() {
        // 1. 获取测试目录下的所有PNG文件
        File dir = new File(TEST_DIR);
        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".png"));

        if (files == null || files.length == 0) {
            System.out.println("测试目录下没有PNG文件：" + TEST_DIR);
            return;
        }

        // 2. 批量上传
        System.out.println("======================================");
        System.out.println("开始批量上传，共 " + files.length + " 个文件");
        System.out.println("======================================");

        for (File file : files) {
            String objectName = ossService.uploadFile(file.getAbsolutePath());
            System.out.println("✅ " + file.getName() + " -> " + objectName);
        }

        System.out.println("======================================");
        System.out.println("批量上传完成！");
        System.out.println("======================================");
    }

    /**
     * 测试3：下载文件
     * <p>
     * 测试场景：
     * 从OSS下载文件到本地
     */
    @Test
    public void test03_downloadFile() {
        // 1. 指定要下载的文件（修改为实际的文件路径）
        String objectName = "2025/01/06/test.png";

        // 2. 指定本地保存路径
        String localFilePath = TEST_DIR + "downloaded_test.png";

        // 3. 下载文件
        ossService.downloadFile(objectName, localFilePath);

        System.out.println("======================================");
        System.out.println("文件下载成功！");
        System.out.println("OSS路径：" + objectName);
        System.out.println("本地路径：" + localFilePath);
        System.out.println("======================================");
    }

    /**
     * 测试4：删除文件
     * <p>
     * 测试场景：
     * 删除OSS中的指定文件
     */
    @Test
    public void test04_deleteFile() {
        // 1. 指定要删除的文件（修改为实际的文件路径）
        String objectName = "2025/01/06/test.png";

        // 2. 删除文件
        ossService.deleteFile(objectName);

        System.out.println("======================================");
        System.out.println("文件删除成功！");
        System.out.println("已删除：" + objectName);
        System.out.println("======================================");
    }

    /**
     * 测试5：批量删除文件
     * <p>
     * 测试场景：
     * 一次删除多个文件
     */
    @Test
    public void test05_batchDelete() {
        // 1. 指定要删除的文件列表（修改为实际的文件路径）
        List<String> objectNames = Arrays.asList(
                "2025/01/06/test1.png",
                "2025/01/06/test2.png",
                "2025/01/06/test3.png");

        // 2. 批量删除
        ossService.deleteFiles(objectNames);

        System.out.println("======================================");
        System.out.println("批量删除成功！");
        System.out.println("已删除 " + objectNames.size() + " 个文件");
        System.out.println("======================================");
    }

    /**
     * 测试6：查询文件列表
     * <p>
     * 测试场景：
     * 1. 查询指定目录下的所有文件
     * 2. 查询整个Bucket的所有文件
     */
    @Test
    public void test06_listFiles() {
        // 1. 查询今天上传的文件（按日期目录）
        String dateFolder = new java.text.SimpleDateFormat("yyyy/MM/dd/")
                .format(new java.util.Date());

        List<String> todayFiles = ossService.listFiles(dateFolder);

        System.out.println("======================================");
        System.out.println("今天的文件（" + dateFolder + "）：");
        for (String file : todayFiles) {
            System.out.println("  - " + file);
        }
        System.out.println("共 " + todayFiles.size() + " 个文件");
        System.out.println("======================================");

        // 2. 查询所有文件
        List<String> allFiles = ossService.listAllFiles();

        System.out.println("======================================");
        System.out.println("所有文件：");
        for (String file : allFiles) {
            System.out.println("  - " + file);
        }
        System.out.println("共 " + allFiles.size() + " 个文件");
        System.out.println("======================================");
    }

    /**
     * 测试7：生成文件访问URL
     * <p>
     * 测试场景：
     * 1. 生成永久访问URL（公共读Bucket）
     * 2. 生成临时访问URL（带签名，有时效）
     */
    @Test
    public void test07_getFileUrl() {
        // 1. 指定文件（修改为实际的文件路径）
        String objectName = "2025/01/06/test.png";

        // 2. 生成永久URL
        String permanentUrl = ossService.getFileUrl(objectName);

        // 3. 生成临时URL（有效期1小时）
        String tempUrl = ossService.getPresignedUrl(objectName, 3600);

        System.out.println("======================================");
        System.out.println("文件访问URL：");
        System.out.println();
        System.out.println("永久URL（适用于公共读Bucket）：");
        System.out.println(permanentUrl);
        System.out.println();
        System.out.println("临时URL（有效期1小时）：");
        System.out.println(tempUrl);
        System.out.println("======================================");
    }

    /**
     * 测试8：检查文件是否存在
     * <p>
     * 测试场景：
     * 验证文件是否已上传到OSS
     */
    @Test
    public void test08_fileExists() {
        // 1. 检查存在的文件
        String existingFile = "2025/01/06/test.png";
        boolean exists1 = ossService.fileExists(existingFile);

        // 2. 检查不存在的文件
        String notExistingFile = "2025/01/06/not_exist.png";
        boolean exists2 = ossService.fileExists(notExistingFile);

        System.out.println("======================================");
        System.out.println("文件存在性检查：");
        System.out.println(existingFile + " -> " + (exists1 ? "✅ 存在" : "❌ 不存在"));
        System.out.println(notExistingFile + " -> " + (exists2 ? "✅ 存在" : "❌ 不存在"));
        System.out.println("======================================");
    }

    /**
     * 测试9：MD5文件完整性校验
     * <p>
     * 测试场景：
     * 1. 上传文件到OSS
     * 2. 下载文件到本地
     * 3. 对比MD5值，验证文件完整性
     */
    @Test
    public void test09_md5Verify() {
        // 1. 上传文件
        String localFilePath = TEST_DIR + "test.png";
        String objectName = ossService.uploadFile(localFilePath);

        System.out.println("======================================");
        System.out.println("开始MD5完整性校验");
        System.out.println("文件：" + objectName);
        System.out.println("======================================");

        // 2. MD5校验
        boolean isValid = ossService.verifyFileMd5(localFilePath, objectName);

        System.out.println("======================================");
        if (isValid) {
            System.out.println("✅ MD5校验通过！文件完整无损。");
        } else {
            System.out.println("❌ MD5校验失败！文件可能损坏。");
        }
        System.out.println("======================================");
    }

    /**
     * 测试10：完整流程测试
     * <p>
     * 测试场景：
     * 上传 → 查询 → 下载 → 校验 → 删除
     */
    @Test
    public void test10_completeWorkflow() {
        System.out.println("\n");
        System.out.println("========================================");
        System.out.println("        完整流程测试开始");
        System.out.println("========================================");

        try {
            // 1. 上传文件
            System.out.println("\n【步骤1】上传文件...");
            String localFilePath = TEST_DIR + "test.png";
            String objectName = ossService.uploadFile(localFilePath);
            System.out.println("✅ 上传成功：" + objectName);

            // 2. 检查文件是否存在
            System.out.println("\n【步骤2】检查文件是否存在...");
            boolean exists = ossService.fileExists(objectName);
            System.out.println(exists ? "✅ 文件存在" : "❌ 文件不存在");

            // 3. 获取文件URL
            System.out.println("\n【步骤3】生成访问URL...");
            String url = ossService.getFileUrl(objectName);
            System.out.println("✅ 访问URL：" + url);

            // 4. 下载文件
            System.out.println("\n【步骤4】下载文件...");
            String downloadPath = TEST_DIR + "workflow_test_download.png";
            ossService.downloadFile(objectName, downloadPath);
            System.out.println("✅ 下载成功：" + downloadPath);

            // 5. MD5校验
            System.out.println("\n【步骤5】MD5完整性校验...");
            boolean isValid = ossService.verifyFileMd5(localFilePath, objectName);
            System.out.println(isValid ? "✅ MD5校验通过" : "❌ MD5校验失败");

            // 6. 删除测试文件（可选）
            // System.out.println("\n【步骤6】删除文件...");
            // ossService.deleteFile(objectName);
            // System.out.println("✅ 删除成功");

            System.out.println("\n========================================");
            System.out.println("        完整流程测试结束");
            System.out.println("========================================\n");

        } catch (Exception e) {
            System.out.println("\n========================================");
            System.out.println("        测试失败");
            System.out.println("========================================");
            e.printStackTrace();
        }
    }
}
