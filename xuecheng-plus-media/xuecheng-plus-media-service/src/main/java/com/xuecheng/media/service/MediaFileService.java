package com.xuecheng.media.service;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.base.model.RestResponse;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * @description 媒资文件管理业务类
 * @author Mr.M
 * @date 2022/9/10 8:55
 * @version 1.0
 */
public interface MediaFileService {

        /**
         * @description 媒资文件查询方法
         * @param companyId           机构ID
         * @param pageParams          分页参数
         * @param queryMediaParamsDto 查询条件
         * @return com.xuecheng.base.model.PageResult<com.xuecheng.media.model.po.MediaFiles>
         * @author Mr.M
         * @date 2022/9/10 8:57
         */
        public PageResult<MediaFiles> queryMediaFiels(Long companyId, PageParams pageParams,
                        QueryMediaParamsDto queryMediaParamsDto);

        /**
         * @description 上传文件
         * @param companyId           机构ID
         * @param uploadFileParamsDto 上传文件参数
         * @param localFilePath       本地文件路径
         * @return UploadFileResultDto 上传结果（包含文件访问地址）
         * @author 学成在线项目组
         * @date 2025-01-06
         */
        public UploadFileResultDto uploadFile(Long companyId, UploadFileParamsDto uploadFileParamsDto,
                        String localFilePath);

        /**
         * @description 将文件信息保存到数据库（事务方法）
         * 
         *              🔥 重要说明：
         *              - 此方法需要暴露为public接口方法
         *              - 原因：需要通过代理对象调用，确保事务生效
         *              - 只包含数据库操作，不包含网络请求
         * 
         * @param companyId           机构ID
         * @param fileMd5             文件MD5值（用作主键）
         * @param uploadFileParamsDto 上传文件参数
         * @param objectName          OSS中的文件路径
         * @return 媒资文件信息对象
         * @author 学成在线项目组
         * @date 2025-01-06
         */
        public MediaFiles addMediaFilesToDb(Long companyId, String fileMd5, UploadFileParamsDto uploadFileParamsDto,
                        String objectName);

        // ============ 大文件分块上传 ============
        /**
         * 检查目标文件是否已存在（用于秒传）
         */
        RestResponse<Boolean> checkFile(String fileMd5);

        /**
         * 检查某个分块是否已存在
         */
        RestResponse<Boolean> checkChunk(String fileMd5, int chunkIndex);

        /**
         * 上传单个分块到本地临时目录
         * 
         * @param fileMd5            文件MD5
         * @param chunk              分块序号
         * @param localChunkFilePath 分块本地临时路径
         */
        RestResponse uploadChunk(String fileMd5, int chunk, String localChunkFilePath);

        /**
         * 合并所有分块、上传至OSS并入库
         * 
         * @param companyId           机构ID
         * @param fileMd5             文件MD5
         * @param chunkTotal          分块总数
         * @param uploadFileParamsDto 文件元信息
         */
        RestResponse mergechunks(Long companyId, String fileMd5, int chunkTotal,
                        UploadFileParamsDto uploadFileParamsDto);

        /**
         * 获取媒资预览路径（对象名/相对路径）。
         * 前端将使用 OSS 前缀拼接成可访问的完整 URL。
         */
        String getPreviewUrl(String mediaId);

}
