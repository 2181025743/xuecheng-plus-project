package com.xuecheng.media.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.*;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 阿里云OSS文件服务
 * <p>
 * 功能：
 * 1. 文件上传（按日期分目录）
 * 2. 文件下载
 * 3. 文件删除
 * 4. 文件列表查询
 * 5. 生成临时访问URL
 * 6. MD5文件完整性校验
 *
 * @author 学成在线项目组
 */
@Slf4j
@Service
public class OssService {

    @Autowired
    private OSS ossClient;

    @Value("${aliyun.oss.endpoint}")
    private String endpoint;

    @Value("${aliyun.oss.bucketName}")
    private String bucketName;

    /**
     * 上传文件到OSS（按日期分目录）
     * <p>
     * 目录结构：2025/01/06/文件名.png
     *
     * @param localFilePath 本地文件路径
     * @return OSS中的文件路径
     */
    public String uploadFile(String localFilePath) {
        try {
            File file = new File(localFilePath);
            if (!file.exists()) {
                throw new RuntimeException("文件不存在：" + localFilePath);
            }

            // 1. 获取文件名和扩展名
            String fileName = file.getName();
            String extension = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf(".")) : "";

            // 2. 按日期生成目录：2025/01/06/
            String dateFolder = getDateFolder();

            // 3. 计算文件MD5值（用作文件名，确保唯一性）
            String fileMd5 = getFileMd5(file);

            // 4. 生成OSS对象名：2025/01/06/MD5值.png
            // 使用MD5值作为文件名，确保文件唯一性，支持去重
            String objectName = dateFolder + fileMd5 + extension;

            // 4. 获取MIME类型
            String contentType = getMimeType(extension);

            // 5. 上传文件
            FileInputStream inputStream = new FileInputStream(file);
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(contentType);
            metadata.setContentLength(file.length());

            ossClient.putObject(bucketName, objectName, inputStream, metadata);
            inputStream.close();

            log.info("文件上传成功：{}", objectName);
            return objectName;

        } catch (Exception e) {
            log.error("文件上传失败：{}", e.getMessage(), e);
            throw new RuntimeException("文件上传失败：" + e.getMessage());
        }
    }

    /**
     * 使用原始文件名决定对象扩展名与ContentType，避免.tmp 后缀。
     */
    public String uploadFile(String localFilePath, String originalFileName) {
        try {
            File file = new File(localFilePath);
            if (!file.exists()) {
                throw new RuntimeException("文件不存在：" + localFilePath);
            }

            String extension = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                extension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }

            String dateFolder = getDateFolder();
            String fileMd5 = getFileMd5(file);
            String objectName = dateFolder + fileMd5 + extension;

            String contentType = getMimeType(extension);

            FileInputStream inputStream = new FileInputStream(file);
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(contentType);
            metadata.setContentLength(file.length());

            ossClient.putObject(bucketName, objectName, inputStream, metadata);
            inputStream.close();

            log.info("文件上传成功：{}", objectName);
            return objectName;
        } catch (Exception e) {
            log.error("文件上传失败：{}", e.getMessage(), e);
            throw new RuntimeException("文件上传失败：" + e.getMessage());
        }
    }

    /**
     * 上传文件并返回访问URL
     *
     * @param localFilePath 本地文件路径
     * @return 文件访问URL
     */
    public String uploadFileWithUrl(String localFilePath) {
        String objectName = uploadFile(localFilePath);
        return getFileUrl(objectName);
    }

    /**
     * 下载文件到本地
     *
     * @param objectName    OSS中的文件路径（如：2025/01/06/test.png）
     * @param localFilePath 本地保存路径
     */
    public void downloadFile(String objectName, String localFilePath) {
        try {
            // 1. 从OSS获取文件
            OSSObject ossObject = ossClient.getObject(bucketName, objectName);
            InputStream inputStream = ossObject.getObjectContent();

            // 2. 保存到本地
            FileOutputStream outputStream = new FileOutputStream(localFilePath);
            IOUtils.copy(inputStream, outputStream);

            // 3. 关闭流
            inputStream.close();
            outputStream.close();

            log.info("文件下载成功：{} -> {}", objectName, localFilePath);

        } catch (Exception e) {
            log.error("文件下载失败：{}", e.getMessage(), e);
            throw new RuntimeException("文件下载失败：" + e.getMessage());
        }
    }

    /**
     * 删除文件
     *
     * @param objectName OSS中的文件路径
     */
    public void deleteFile(String objectName) {
        try {
            ossClient.deleteObject(bucketName, objectName);
            log.info("文件删除成功：{}", objectName);

        } catch (Exception e) {
            log.error("文件删除失败：{}", e.getMessage(), e);
            throw new RuntimeException("文件删除失败：" + e.getMessage());
        }
    }

    /**
     * 批量删除文件
     *
     * @param objectNames 文件路径列表
     */
    public void deleteFiles(List<String> objectNames) {
        try {
            DeleteObjectsRequest request = new DeleteObjectsRequest(bucketName)
                    .withKeys(objectNames);
            DeleteObjectsResult result = ossClient.deleteObjects(request);

            log.info("批量删除文件成功，删除数量：{}", result.getDeletedObjects().size());

        } catch (Exception e) {
            log.error("批量删除文件失败：{}", e.getMessage(), e);
            throw new RuntimeException("批量删除文件失败：" + e.getMessage());
        }
    }

    /**
     * 列出指定目录下的所有文件
     *
     * @param prefix 目录前缀（如：2025/01/06/）
     * @return 文件列表
     */
    public List<String> listFiles(String prefix) {
        List<String> fileList = new ArrayList<>();
        try {
            ListObjectsRequest request = new ListObjectsRequest(bucketName);
            request.setPrefix(prefix);
            request.setMaxKeys(1000);

            ObjectListing listing = ossClient.listObjects(request);
            for (OSSObjectSummary summary : listing.getObjectSummaries()) {
                fileList.add(summary.getKey());
            }

            log.info("查询文件列表成功，目录：{}，文件数量：{}", prefix, fileList.size());
            return fileList;

        } catch (Exception e) {
            log.error("查询文件列表失败：{}", e.getMessage(), e);
            throw new RuntimeException("查询文件列表失败：" + e.getMessage());
        }
    }

    /**
     * 列出所有文件
     *
     * @return 文件列表
     */
    public List<String> listAllFiles() {
        return listFiles("");
    }

    /**
     * 生成文件访问URL（永久链接，适用于公共读的Bucket）
     *
     * @param objectName OSS中的文件路径
     * @return 访问URL
     */
    public String getFileUrl(String objectName) {
        // 格式：https://yangxiaobucker.oss-cn-beijing.aliyuncs.com/2025/01/06/test.png
        return "https://" + bucketName + "." + endpoint + "/" + objectName;
    }

    /**
     * 生成临时访问URL（带签名，有时效性）
     *
     * @param objectName    OSS中的文件路径
     * @param expireSeconds 过期时间（秒）
     * @return 临时访问URL
     */
    public String getPresignedUrl(String objectName, long expireSeconds) {
        try {
            Date expiration = new Date(System.currentTimeMillis() + expireSeconds * 1000);
            URL url = ossClient.generatePresignedUrl(bucketName, objectName, expiration);
            return url.toString();

        } catch (Exception e) {
            log.error("生成临时URL失败：{}", e.getMessage(), e);
            throw new RuntimeException("生成临时URL失败：" + e.getMessage());
        }
    }

    /**
     * 检查文件是否存在
     *
     * @param objectName OSS中的文件路径
     * @return true-存在，false-不存在
     */
    public boolean fileExists(String objectName) {
        try {
            return ossClient.doesObjectExist(bucketName, objectName);
        } catch (Exception e) {
            log.error("检查文件是否存在失败：{}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 获取文件信息
     *
     * @param objectName OSS中的文件路径
     * @return 文件元信息
     */
    public ObjectMetadata getFileInfo(String objectName) {
        try {
            return ossClient.getObjectMetadata(bucketName, objectName);
        } catch (Exception e) {
            log.error("获取文件信息失败：{}", e.getMessage(), e);
            throw new RuntimeException("获取文件信息失败：" + e.getMessage());
        }
    }

    /**
     * MD5文件完整性校验
     *
     * @param localFilePath 本地文件路径
     * @param objectName    OSS中的文件路径
     * @return true-完整，false-损坏
     */
    public boolean verifyFileMd5(String localFilePath, String objectName) {
        try {
            // 1. 计算本地文件MD5
            FileInputStream localStream = new FileInputStream(localFilePath);
            String localMd5 = DigestUtils.md5Hex(localStream);
            localStream.close();

            // 2. 下载OSS文件到临时目录
            String tempFilePath = System.getProperty("java.io.tmpdir") + "/" +
                    objectName.replace("/", "_");
            downloadFile(objectName, tempFilePath);

            // 3. 计算下载文件MD5
            FileInputStream downloadStream = new FileInputStream(tempFilePath);
            String downloadMd5 = DigestUtils.md5Hex(downloadStream);
            downloadStream.close();

            // 4. 删除临时文件
            new File(tempFilePath).delete();

            // 5. 对比MD5
            boolean isMatch = localMd5.equals(downloadMd5);
            log.info("MD5校验结果：{}，本地MD5：{}，远程MD5：{}",
                    isMatch ? "通过" : "失败", localMd5, downloadMd5);

            return isMatch;

        } catch (Exception e) {
            log.error("MD5校验失败：{}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 获取当前日期目录：2025/01/06/
     */
    private String getDateFolder() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd/");
        return sdf.format(new Date());
    }

    /**
     * 根据扩展名获取MIME类型
     */
    private String getMimeType(String extension) {
        if (extension == null) {
            extension = "";
        }

        // 默认MIME类型：通用字节流
        String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;

        // 根据扩展名查找MIME类型
        ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(extension);
        if (extensionMatch != null) {
            mimeType = extensionMatch.getMimeType();
        }

        return mimeType;
    }

    /**
     * 计算文件MD5值
     *
     * @param file 文件
     * @return MD5值
     */
    private String getFileMd5(File file) {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            return DigestUtils.md5Hex(fileInputStream);
        } catch (Exception e) {
            log.error("计算文件MD5失败", e);
            throw new RuntimeException("计算文件MD5失败");
        }
    }
}
