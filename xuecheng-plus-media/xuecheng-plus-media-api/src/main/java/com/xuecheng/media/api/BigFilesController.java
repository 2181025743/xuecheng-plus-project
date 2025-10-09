package com.xuecheng.media.api;

import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.service.MediaFileService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

/**
 * 大文件上传控制器
 * 
 * 功能说明:
 * 该控制器实现了大文件分块上传的完整流程,主要包括:
 * 1. 文件秒传检查 - 检查文件是否已存在,避免重复上传
 * 2. 分块检查 - 检查某个分块是否已上传,支持断点续传
 * 3. 分块上传 - 上传单个文件分块
 * 4. 分块合并 - 将所有分块合并成完整文件
 * 
 * 技术要点:
 * - 使用 MD5 作为文件唯一标识,实现秒传和去重
 * - 支持断点续传,提高上传可靠性
 * - 分块上传降低单次请求大小,提高上传成功率
 */
@Api(value = "大文件上传接口", tags = "大文件分块上传、合并")
@RestController
@RequestMapping("/upload")
public class BigFilesController {

    @Autowired
    private MediaFileService mediaFileService;

    /**
     * 检查文件是否已存在(秒传功能)
     * 
     * 工作原理:
     * 1. 前端计算文件的 MD5 值
     * 2. 调用此接口检查该 MD5 对应的文件是否已存在于系统中
     * 3. 如果存在,直接返回成功,无需重新上传(秒传)
     * 4. 如果不存在,前端继续执行分块上传流程
     * 
     * @param fileMd5 文件的 MD5 值,作为文件的唯一标识
     * @return RestResponse<Boolean> true-文件已存在, false-文件不存在
     */
    @ApiOperation("检查文件是否已存在(秒传)")
    @RequestMapping(value = "/checkfile", method = { RequestMethod.GET, RequestMethod.POST })
    public RestResponse<Boolean> checkfile(@RequestParam("fileMd5") String fileMd5) {
        return mediaFileService.checkFile(fileMd5);
    }

    /**
     * 检查指定分块是否已存在
     * 
     * 工作原理:
     * 1. 在上传每个分块之前,先调用此接口检查该分块是否已上传
     * 2. 如果已存在,跳过该分块的上传(断点续传)
     * 3. 如果不存在,继续上传该分块
     * 
     * 应用场景:
     * - 网络中断后恢复上传,无需重新上传已完成的分块
     * - 提高上传效率和用户体验
     * 
     * @param fileMd5 文件的 MD5 值
     * @param chunk   分块序号(从0开始)
     * @return RestResponse<Boolean> true-分块已存在, false-分块不存在
     */
    @ApiOperation("检查分块是否已存在")
    @RequestMapping(value = "/checkchunk", method = { RequestMethod.GET, RequestMethod.POST })
    public RestResponse<Boolean> checkchunk(@RequestParam("fileMd5") String fileMd5,
            @RequestParam("chunk") int chunk) {
        return mediaFileService.checkChunk(fileMd5, chunk);
    }

    /**
     * 上传文件分块
     * 
     * 工作流程:
     * 1. 接收前端上传的分块文件
     * 2. 将分块临时保存到系统临时目录
     * 3. 调用 service 层将分块上传到 MinIO 存储
     * 4. 删除本地临时文件,释放磁盘空间
     * 
     * 技术细节:
     * - 使用系统临时目录存储分块,避免占用项目空间
     * - 使用纳秒时间戳生成唯一文件名,避免文件名冲突
     * - 上传完成后立即删除临时文件,节省磁盘空间
     * 
     * @param file    分块文件对象
     * @param fileMd5 文件的 MD5 值
     * @param chunk   当前分块序号
     * @return RestResponse 上传结果
     */
    @ApiOperation("上传分块")
    @PostMapping("/uploadchunk")
    public RestResponse uploadchunk(@RequestParam("file") MultipartFile file,
            @RequestParam("fileMd5") String fileMd5,
            @RequestParam("chunk") int chunk) {
        try {
            // 获取系统临时目录路径
            String tempDir = System.getProperty("java.io.tmpdir");
            // 生成唯一的临时文件名,使用纳秒时间戳确保唯一性
            String localFilePath = tempDir + File.separator + "xc-chunk-" + System.nanoTime();
            // 创建临时文件对象
            File temp = new File(localFilePath);
            // 将上传的分块保存到临时文件
            file.transferTo(temp);
            // 调用 service 层上传分块到 MinIO
            RestResponse resp = mediaFileService.uploadChunk(fileMd5, chunk, localFilePath);
            // 删除临时文件,释放磁盘空间
            temp.delete();
            return resp;
        } catch (Exception e) {
            // 捕获异常,返回友好的错误信息
            return RestResponse.validfail(false, "上传分块失败");
        }
    }

    /**
     * 合并文件分块
     * 
     * 工作流程:
     * 1. 所有分块上传完成后,前端调用此接口
     * 2. 后端从 MinIO 下载所有分块
     * 3. 按顺序合并所有分块为完整文件
     * 4. 将完整文件上传到 MinIO 的正式存储位置
     * 5. 将文件信息保存到数据库
     * 6. 删除临时分块文件
     * 
     * 参数说明:
     * - fileMd5: 文件的 MD5 值,用于定位分块存储位置
     * - fileName: 原始文件名
     * - chunkTotal: 分块总数,用于验证是否所有分块都已上传
     * - fileType: 文件类型(可选),默认为视频类型 001002
     * 
     * 业务逻辑:
     * - 设置默认的上传用户为"系统管理员"
     * - 设置默认的机构 ID 为 1232141425
     * - 如果未指定文件类型,默认为视频类型
     * 
     * @param fileMd5    文件的 MD5 值
     * @param fileName   文件名
     * @param chunkTotal 分块总数
     * @param fileType   文件类型(可选)
     * @return RestResponse 合并结果,包含文件信息
     */
    @ApiOperation("合并分块")
    @PostMapping("/mergechunks")
    public RestResponse mergechunks(@RequestParam("fileMd5") String fileMd5,
            @RequestParam("fileName") String fileName,
            @RequestParam("chunkTotal") int chunkTotal,
            @RequestParam(value = "fileType", required = false) String fileType,
            @RequestParam(value = "fileSize", required = false) Long fileSize) {
        // 构建上传文件参数对象
        UploadFileParamsDto params = new UploadFileParamsDto();
        params.setFilename(fileName);
        // 设置文件类型,如果未指定则默认为视频类型 001002
        params.setFileType(fileType != null ? fileType : "001002");
        // 设置上传用户
        params.setUsername("系统管理员");
        // 设置备注信息
        params.setRemark("大文件分块合并");
        // 设置文件大小（如果前端传递了）
        if (fileSize != null && fileSize > 0) {
            params.setFileSize(fileSize);
        }
        // 设置机构 ID
        Long companyId = 1232141425L;
        // 调用 service 层执行分块合并
        return mediaFileService.mergechunks(companyId, fileMd5, chunkTotal, params);
    }
}
