package com.xuecheng.media;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 大文件上传测试
 * 测试文件分块和合并功能
 * <p>
 * 目的：理解断点续传的原理
 * 1. 将大文件分成多个小块
 * 2. 分块上传（断点续传时可以从中断的块继续）
 * 3. 上传完成后合并所有分块
 * 4. 校验合并后的文件是否完整
 */
public class BigFileTest {

    /**
     * 测试文件分块
     * 将一个大文件分成多个小文件块
     */
    @Test
    public void testChunk() throws IOException {
        // 源文件路径（假设有一个视频文件）
        File sourceFile = new File("D:/develop/upload/2.mp4");

        // 分块文件存储路径
        String chunkFilePath = "D:/develop/upload/chunk/";

        // 分块大小：5MB = 5 * 1024 * 1024 字节
        int chunkSize = 5 * 1024 * 1024;

        // 计算分块数量（向上取整）
        int chunkNum = (int) Math.ceil(sourceFile.length() * 1.0 / chunkSize);

        System.out.println("源文件大小：" + sourceFile.length() + " 字节");
        System.out.println("分块大小：" + chunkSize + " 字节");
        System.out.println("分块数量：" + chunkNum);

        // 使用RandomAccessFile读取源文件（支持随机读取）
        RandomAccessFile raf_r = new RandomAccessFile(sourceFile, "r");

        // 缓冲区
        byte[] bytes = new byte[1024];

        // 循环生成每个分块文件
        for (int i = 0; i < chunkNum; i++) {
            // 创建分块文件，命名为 0, 1, 2, 3...
            File chunkFile = new File(chunkFilePath + i);

            // 创建写入流
            RandomAccessFile raf_rw = new RandomAccessFile(chunkFile, "rw");

            int len = -1;
            // 读取数据并写入分块文件
            while ((len = raf_r.read(bytes)) != -1) {
                raf_rw.write(bytes, 0, len);

                // 如果当前分块文件大小达到限制，停止写入
                if (chunkFile.length() >= chunkSize) {
                    break;
                }
            }

            raf_rw.close();
            System.out.println("分块 " + i + " 生成完成，大小：" + chunkFile.length());
        }

        raf_r.close();
        System.out.println("文件分块完成！");
    }

    /**
     * 测试文件合并
     * 将多个分块文件合并成一个完整文件
     */
    @Test
    public void testMerge() throws IOException {
        // 分块文件目录
        File chunkFolder = new File("D:/develop/upload/chunk/");

        // 源文件（用于校验）
        File sourceFile = new File("D:/develop/upload/1.mp4");

        // 合并后的文件
        File mergeFile = new File("D:/develop/upload/1_merge.mp4");

        // 获取所有分块文件
        File[] files = chunkFolder.listFiles();

        // 将文件数组转为List并排序（重要！必须按照0,1,2,3...顺序合并）
        List<File> fileList = Arrays.asList(files);
        Collections.sort(fileList, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                // 按文件名（数字）升序排列
                return Integer.parseInt(o1.getName()) - Integer.parseInt(o2.getName());
            }
        });

        System.out.println("开始合并文件，共 " + fileList.size() + " 个分块");

        // 创建合并文件的写入流
        RandomAccessFile raf_rw = new RandomAccessFile(mergeFile, "rw");

        // 缓冲区
        byte[] bytes = new byte[1024];

        // 遍历分块文件，依次写入合并文件
        for (File chunkFile : fileList) {
            // 读取分块文件
            RandomAccessFile raf_r = new RandomAccessFile(chunkFile, "r");

            int len = -1;
            while ((len = raf_r.read(bytes)) != -1) {
                raf_rw.write(bytes, 0, len);
            }

            raf_r.close();
            System.out.println("已合并分块：" + chunkFile.getName());
        }

        raf_rw.close();

        System.out.println("文件合并完成！");

        // 校验：比较源文件和合并文件的MD5值
        FileInputStream sourceFileStream = new FileInputStream(sourceFile);
        FileInputStream mergeFileStream = new FileInputStream(mergeFile);

        String sourceMd5 = DigestUtils.md5Hex(sourceFileStream);
        String mergeMd5 = DigestUtils.md5Hex(mergeFileStream);

        if (sourceMd5.equals(mergeMd5)) {
            System.out.println("文件合并成功！MD5值相同");
        } else {
            System.out.println("文件合并失败！MD5值不同");
            System.out.println("源文件MD5: " + sourceMd5);
            System.out.println("合并文件MD5: " + mergeMd5);
        }

        sourceFileStream.close();
        mergeFileStream.close();
    }

    /**
     * 测试：计算文件MD5值
     * 用于获取测试文件的MD5，供分块上传接口使用
     */
    @Test
    public void testFileMd5() throws Exception {
        File file = new File("D:/develop/upload/2.mp4");
        FileInputStream fis = new FileInputStream(file);
        String md5 = DigestUtils.md5Hex(fis);
        fis.close();

        System.out.println("========================================");
        System.out.println("文件MD5值：" + md5);
        System.out.println("文件大小：" + file.length() + " 字节");
        System.out.println("========================================");
    }
}
