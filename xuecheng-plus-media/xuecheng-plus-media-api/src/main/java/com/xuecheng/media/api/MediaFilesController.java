package com.xuecheng.media.api;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.service.MediaFileService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

/**
 * @author Mr.M
 * @version 1.0
 * @description 媒资文件管理接口
 * @date 2022/9/6 11:29
 */
@Api(value = "媒资文件管理接口", tags = "媒资文件管理接口")
@RestController
public class MediaFilesController {

    @Autowired
    MediaFileService mediaFileService;

    @ApiOperation("媒资列表查询接口")
    @PostMapping("/files")
    public PageResult<MediaFiles> list(PageParams pageParams, @RequestBody QueryMediaParamsDto queryMediaParamsDto) {
        Long companyId = 1232141425L;
        return mediaFileService.queryMediaFiels(companyId, pageParams, queryMediaParamsDto);
    }

    /**
     * 上传文件接口
     * <p>
     * 功能：
     * 1. 接收前端上传的文件
     * 2. 调用媒资服务上传到OSS
     * 3. 返回文件访问地址
     * <p>
     * 支持文件去重：
     * - 相同文件（MD5相同）只上传一次
     * - 第二次上传直接返回已有文件地址（秒传）
     *
     * @param filedata 上传的文件
     * @param fileType 文件类型（001001:图片 001002:视频 001003:文档）
     * @return 上传结果（包含文件访问地址）
     */
    @ApiOperation("上传文件接口")
    @PostMapping("/upload/coursefile")
    public UploadFileResultDto upload(@RequestPart("filedata") MultipartFile filedata,
            @RequestParam(value = "fileType", required = false) String fileType) {
        Long companyId = 1232141425L;
        // 2. 构建上传参数
        UploadFileParamsDto uploadFileParamsDto = new UploadFileParamsDto();
        uploadFileParamsDto.setFilename(filedata.getOriginalFilename());
        uploadFileParamsDto.setFileSize(filedata.getSize());
        uploadFileParamsDto.setFileType(fileType != null ? fileType : "001001"); // 默认图片类型
        uploadFileParamsDto.setUsername("系统管理员"); // 暂时写死，后续从登录用户获取
        try {
            String tempDir = System.getProperty("java.io.tmpdir");
            String tempFilePath = tempDir + File.separator + filedata.getOriginalFilename();
            File tempFile = new File(tempFilePath);
            filedata.transferTo(tempFile);
            UploadFileResultDto resultDto = mediaFileService.uploadFile(companyId, uploadFileParamsDto, tempFilePath);
            tempFile.delete();
            return resultDto;
        } catch (IOException e) {
            throw new RuntimeException("上传文件失败：" + e.getMessage());
        }
    }

    /**
     * 预览接口：返回对象路径（相对路径），前端自行用 OSS 前缀拼接
     */
    @ApiOperation("预览媒资对象路径")
    @GetMapping("/preview/{mediaId}")
    public String preview(@PathVariable("mediaId") String mediaId) {
        return mediaFileService.getPreviewUrl(mediaId);
    }

}
