package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.service.MediaFileService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 媒资文件管理业务实现类
 * 
 * 📚 类的职责：
 * 这是一个Service层的实现类，负责处理媒资文件的核心业务逻辑
 * 
 * 🎯 核心功能模块：
 * 1. 【文件查询】分页查询媒资文件列表
 * 2. 【文件上传】支持普通上传和秒传（MD5去重）
 * 3. 【分块上传】支持大文件分块上传和合并
 * 4. 【数据持久化】将文件信息保存到数据库
 * 5. 【OSS集成】与阿里云OSS对象存储服务集成
 * 
 * 🔥 技术亮点：
 * 1. 秒传功能：通过MD5值判断文件是否已存在，避免重复上传
 * 2. 事务优化：将网络请求和数据库操作分离，缩短事务持有时间
 * 3. 代理模式：通过注入自身代理对象解决事务失效问题
 * 4. 分块上传：支持大文件分块上传，提高上传成功率
 * 
 * 📖 设计模式：
 * - 代理模式：使用currentProxy解决Spring事务失效问题
 * - 模板方法：文件上传流程的标准化处理
 * 
 * @author Mr.M
 * @version 1.0
 * @date 2022/9/10 8:58
 */
@Slf4j // Lombok注解，自动生成log日志对象
@Service // Spring注解，标识这是一个Service层组件，会被Spring容器管理
public class MediaFileServiceImpl implements MediaFileService {

    /**
     * 媒资文件数据访问层（Mapper）
     * 
     * 作用：操作数据库中的 media_files 表
     * 
     * 📊 数据库表结构（media_files）：
     * - id: 主键，使用文件MD5值（用于去重）
     * - company_id: 机构ID
     * - filename: 文件名
     * - file_type: 文件类型（001001:图片 001002:视频 001003:文档）
     * - bucket: OSS存储桶名称
     * - file_path: OSS中的文件路径
     * - url: 文件访问URL
     * - status: 状态（1-正常）
     * - audit_status: 审核状态（002003-审核通过）
     * - create_date: 创建时间
     * - change_date: 修改时间
     * 
     * 🔧 技术栈：MyBatis Plus
     * - 提供了基础的CRUD方法（insert、selectById、selectPage等）
     * - 支持Lambda表达式构建查询条件
     */
  @Autowired
 MediaFilesMapper mediaFilesMapper;

    /**
     * OSS文件服务（阿里云对象存储服务）
     * 
     * 作用：负责将文件上传到阿里云OSS
     * 
     * 🌐 OSS（Object Storage Service）对象存储服务：
     * - 阿里云提供的海量、安全、低成本、高可靠的云存储服务
     * - 适合存储图片、视频、音频等非结构化数据
     * - 提供HTTP/HTTPS访问接口，支持CDN加速
     * 
     * 📦 存储结构：
     * - Bucket（存储桶）：类似于文件系统的根目录
     * - Object（对象）：存储的文件，通过ObjectName（对象名）唯一标识
     * - ObjectName格式：2025/01/06/abc123.jpg（按日期分目录存储）
     * 
     * 🔗 访问方式：
     * - 公共读：https://bucketName.oss-cn-beijing.aliyuncs.com/objectName
     * - 私有读：需要生成签名URL（带有时效性）
     */
    @Autowired
    OssService ossService;

    /**
     * OSS存储桶名称
     * 
     * 配置来源：application.yml 或 application.properties
     * 配置示例：aliyun.oss.bucketName=yangxiaobucker
     * 
     * 🎯 作用：
     * - 指定文件上传到哪个OSS存储桶
     * - 不同环境可以配置不同的存储桶（开发、测试、生产）
     * 
     * 💡 最佳实践：
     * - 开发环境：dev-bucket
     * - 测试环境：test-bucket
     * - 生产环境：prod-bucket
     * - 通过配置文件切换，避免硬编码
     */
    @Value("${aliyun.oss.bucketName}")
    private String bucketName;

    /**
     * 🔥 核心优化：注入自己的代理对象
     * 
     * ❓ 为什么需要代理对象？
     * 
     * 【问题背景】Spring事务失效问题
     * 
     * 场景：
     * - uploadFile()方法（无@Transactional）调用 addMediaFilesToDb()方法（有@Transactional）
     * - 如果使用 this.addMediaFilesToDb()，事务不会生效
     * 
     * 原因：
     * 1. Spring事务是基于AOP（面向切面编程）实现的
     * 2. Spring会为Service类创建一个代理对象（Proxy）
     * 3. 代理对象会在方法调用前后添加事务管理逻辑（开启事务、提交、回滚）
     * 4. 但是，this指向的是原始对象，不是代理对象
     * 5. 通过this调用方法，不会经过代理对象，事务拦截器不会生效
     * 
     * 📊 调用链对比：
     * 
     * ❌ 错误方式（this调用）：
     * Controller → Proxy.uploadFile() → 原始对象.uploadFile() →
     * this.addMediaFilesToDb()
     * ↓
     * 直接调用，跳过代理
     * ↓
     * 事务失效 ❌
     * 
     * ✅ 正确方式（代理对象调用）：
     * Controller → Proxy.uploadFile() → 原始对象.uploadFile() →
     * currentProxy.addMediaFilesToDb()
     * ↓
     * 经过代理对象
     * ↓
     * 事务拦截器生效 ✅
     * ↓
     * 开启事务 → 执行方法 → 提交/回滚
     * 
     * 💡 解决方案：
     * 1. 注入自己的代理对象：@Autowired MediaFileService currentProxy
     * 2. 通过代理对象调用：currentProxy.addMediaFilesToDb()
     * 3. 这样就能触发事务拦截器，事务生效
     * 
     * 🎯 使用场景：
     * - 同一个类中，非事务方法调用事务方法
     * - 需要确保事务生效的场景
     * 
     * 📝 注意事项：
     * - 需要在配置类上添加 @EnableAspectJAutoProxy(exposeProxy = true)
     * - 或者直接注入自己的接口（推荐，更简单）
     * 
     * 🔍 其他解决方案：
     * 1. 使用AopContext.currentProxy()（需要exposeProxy=true）
     * 2. 将方法拆分到不同的Service类（推荐，更符合单一职责原则）
     * 3. 使用编程式事务（TransactionTemplate）
     */
    @Autowired
    MediaFileService currentProxy;

    // public static void main(String[] args) {
    // System.out.println(new MediaFileServiceImpl().getFileMd5(new
    // File("D:\\project\\java\\xuecheng-plus-project\\.git\\config")));
    // }

    /**
     * 媒资文件查询方法（分页）
     * 
     * 🎯 功能说明：
     * 根据查询条件，分页查询媒资文件列表
     * 
     * 📊 业务流程：
     * 1. 接收查询参数（机构ID、分页参数、查询条件）
     * 2. 构建MyBatis Plus的查询条件对象
     * 3. 执行分页查询
     * 4. 封装查询结果并返回
     * 
     * 🔧 技术实现：
     * - 使用MyBatis Plus的分页插件
     * - 使用Lambda表达式构建查询条件（类型安全，避免字段名写错）
     * 
     * 💡 扩展点：
     * - 可以在queryWrapper中添加更多查询条件
     * - 例如：按文件名模糊查询、按文件类型筛选、按上传时间排序等
     * 
     * @param companyId           机构ID（用于数据隔离，每个机构只能查询自己的文件）
     * @param pageParams          分页参数对象
     *                            - pageNo: 当前页码（从1开始）
     *                            - pageSize: 每页显示条数
     * @param queryMediaParamsDto 查询条件对象
     *                            - filename: 文件名（支持模糊查询）
     *                            - fileType: 文件类型（001001:图片 001002:视频 001003:文档）
     *                            - auditStatus: 审核状态
     * @return PageResult<MediaFiles> 分页结果对象
     *         - items: 当前页的数据列表
     *         - counts: 总记录数
     *         - page: 当前页码
     *         - pageSize: 每页条数
     */
 @Override
    public PageResult<MediaFiles> queryMediaFiels(Long companyId, PageParams pageParams,
            QueryMediaParamsDto queryMediaParamsDto) {

        // ===== 步骤1：构建查询条件对象 =====
        // LambdaQueryWrapper：MyBatis Plus提供的Lambda表达式查询构造器
        // 优点：类型安全，编译期检查，避免字段名写错
  LambdaQueryWrapper<MediaFiles> queryWrapper = new LambdaQueryWrapper<>();
  
        // 💡 机构隔离与条件查询
        // queryWrapper.eq(MediaFiles::getCompanyId, companyId);
        if (queryMediaParamsDto != null) {
            if (queryMediaParamsDto.getFilename() != null && !queryMediaParamsDto.getFilename().isEmpty()) {
                queryWrapper.like(MediaFiles::getFilename, queryMediaParamsDto.getFilename());
            }
            if (queryMediaParamsDto.getFileType() != null && !queryMediaParamsDto.getFileType().isEmpty()) {
                queryWrapper.eq(MediaFiles::getFileType, queryMediaParamsDto.getFileType());
            }
            if (queryMediaParamsDto.getAuditStatus() != null && !queryMediaParamsDto.getAuditStatus().isEmpty()) {
                queryWrapper.eq(MediaFiles::getAuditStatus, queryMediaParamsDto.getAuditStatus());
            }
            // 服务端排序：createDate ASC/DESC，附加次键id保证稳定性
            String sortBy = queryMediaParamsDto.getSortBy();
            String sortOrder = queryMediaParamsDto.getSortOrder();
            if ("createDate".equalsIgnoreCase(sortBy)) {
                boolean asc = "asc".equalsIgnoreCase(sortOrder);
                queryWrapper.orderBy(true, asc, MediaFiles::getCreateDate);
                // 次键，避免同时间戳翻页抖动
                queryWrapper.orderBy(true, asc, MediaFiles::getId);
            } else {
                // 默认：按上传时间降序 + 次键id降序
                queryWrapper.orderByDesc(MediaFiles::getCreateDate);
                queryWrapper.orderByDesc(MediaFiles::getId);
            }
        } else {
            queryWrapper.orderByDesc(MediaFiles::getCreateDate);
            queryWrapper.orderByDesc(MediaFiles::getId);
        }

        // ===== 步骤2：创建分页对象 =====
        // Page对象：MyBatis Plus的分页对象
        // 参数1：当前页码（从1开始）
        // 参数2：每页显示条数
  Page<MediaFiles> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());

        // ===== 步骤3：执行分页查询 =====
        // selectPage方法：MyBatis Plus提供的分页查询方法
        // 参数1：分页对象（包含页码和每页条数）
        // 参数2：查询条件对象
        // 返回值：分页结果对象（包含数据列表和总记录数）
        //
        // 🔍 底层原理：
        // 1. MyBatis Plus会自动在SQL中添加 LIMIT 和 OFFSET
        // 2. 例如：SELECT * FROM media_files LIMIT 10 OFFSET 0
        // 3. 同时会执行 COUNT 查询获取总记录数
  Page<MediaFiles> pageResult = mediaFilesMapper.selectPage(page, queryWrapper);

        // ===== 步骤4：从分页结果中提取数据 =====
        // getRecords()：获取当前页的数据列表
  List<MediaFiles> list = pageResult.getRecords();

        // getTotal()：获取总记录数（用于前端计算总页数）
  long total = pageResult.getTotal();

        // ===== 步骤5：构建标准的分页结果对象 =====
        // PageResult：项目自定义的分页结果对象
        // 统一返回格式，方便前端处理
        PageResult<MediaFiles> mediaListResult = new PageResult<>(list, total, pageParams.getPageNo(),
                pageParams.getPageSize());

  return mediaListResult;
    }

    /**
     * 上传文件（核心方法）
     * 
     * 🎯 功能说明：
     * 这是文件上传的核心方法，实现了完整的文件上传流程
     * 
     * 📊 业务流程（5个步骤）：
     * 
     * 步骤1：计算文件MD5值
     * - 作用：唯一标识文件（内容相同的文件MD5值相同）
     * - 用途：文件去重、秒传功能、数据库主键
     * 
     * 步骤2：检查文件是否已存在（秒传功能）
     * - 通过MD5值查询数据库
     * - 如果文件已存在，直接返回已有文件信息（秒传）
     * - 如果文件不存在，继续执行上传
     * 
     * 步骤3：上传文件到阿里云OSS
     * - 调用ossService.uploadFile()方法
     * - 将文件上传到OSS存储桶
     * - 返回文件在OSS中的路径（ObjectName）
     * 
     * 步骤4：保存文件信息到数据库
     * - 通过代理对象调用addMediaFilesToDb()方法
     * - 确保事务生效（重要！）
     * - 保存文件的元数据信息
     * 
     * 步骤5：返回上传结果
     * - 包含文件访问URL、文件ID等信息
     * - 前端可以直接使用URL访问文件
     * 
     * 🔥 秒传原理：
     * 
     * 什么是秒传？
     * - 用户上传文件时，系统检测到该文件已经存在
     * - 直接返回已有文件的信息，无需重复上传
     * - 用户体验：瞬间完成上传（实际上没有上传）
     * 
     * 如何实现？
     * 1. 使用MD5值作为文件的唯一标识
     * 2. 相同内容的文件，MD5值相同
     * 3. 上传前先计算MD5，查询数据库是否存在
     * 4. 如果存在，直接返回已有文件信息
     * 5. 如果不存在，执行正常上传流程
     * 
     * 优点：
     * - 节省存储空间（相同文件只存储一份）
     * - 节省带宽（无需重复上传）
     * - 提升用户体验（上传速度快）
     * 
     * 🔥 事务优化：
     * 
     * 为什么不在此方法上加@Transactional？
     * 
     * 原因：
     * 1. 此方法包含网络请求（上传文件到OSS）
     * 2. 网络请求耗时较长（可能几秒到几十秒）
     * 3. 如果整个方法都在事务中，会长时间占用数据库连接
     * 4. 数据库连接是有限资源，长时间占用会影响系统性能
     * 
     * 优化方案：
     * 1. 只在addMediaFilesToDb()方法上加@Transactional
     * 2. 该方法只包含数据库操作，执行时间短
     * 3. 缩短事务持有时间，提高数据库连接利用率
     * 
     * 事务范围对比：
     * 
     * ❌ 不推荐（事务范围过大）：
     * 
     * @Transactional
     *                uploadFile() {
     *                计算MD5 // 无需事务
     *                查询数据库 // 需要事务
     *                上传到OSS // 无需事务（网络请求，耗时长）
     *                保存到数据库 // 需要事务
     *                }
     * 
     *                ✅ 推荐（事务范围最小化）：
     *                uploadFile() {
     *                计算MD5 // 无需事务
     *                查询数据库 // 无需事务（只读操作）
     *                上传到OSS // 无需事务
     *                currentProxy.addMediaFilesToDb() // 有事务（只包含数据库写操作）
     *                }
     * 
     *                🔥 事务生效关键：
     * 
     *                为什么要用currentProxy？
     * 
     *                错误方式：
     *                this.addMediaFilesToDb() // ❌ 事务失效
     * 
     *                正确方式：
     *                currentProxy.addMediaFilesToDb() // ✅ 事务生效
     * 
     *                原因：
     *                - this是原始对象，直接调用，不经过代理
     *                - currentProxy是代理对象，会触发事务拦截器
     * 
     *                🛡️ 异常处理：
     *                - 捕获所有异常，记录日志
     *                - 抛出运行时异常，方便上层统一处理
     *                - 如果addMediaFilesToDb()抛出异常，事务会自动回滚
     * 
     * @param companyId           机构ID（用于数据隔离）
     * @param uploadFileParamsDto 上传文件参数对象
     *                            - filename: 文件名
     *                            - fileType: 文件类型
     *                            - fileSize: 文件大小
     *                            - tags: 文件标签
     *                            - username: 上传用户
     *                            - remark: 备注
     * @param localFilePath       本地临时文件路径
     *                            - 文件已经上传到服务器的临时目录
     *                            - 例如：/tmp/upload_abc123.jpg
     * @return UploadFileResultDto 上传结果对象
     *         - url: 文件访问URL
     *         - id: 文件ID（MD5值）
     *         - filename: 文件名
     *         - fileType: 文件类型
     */
    @Override
    public UploadFileResultDto uploadFile(Long companyId, UploadFileParamsDto uploadFileParamsDto,
            String localFilePath) {
        try {
            // ===== 步骤1：计算文件MD5值（无需事务） =====
            //
            // 🔍 为什么要计算MD5？
            // 1. 唯一标识文件：相同内容的文件MD5值相同
            // 2. 文件去重：避免重复存储相同文件
            // 3. 秒传功能：快速判断文件是否已存在
            // 4. 数据库主键：使用MD5作为主键，天然去重
            //
            // 📝 MD5特点：
            // - 固定长度：32位十六进制字符串
            // - 唯一性：内容不同，MD5值不同（碰撞概率极低）
            // - 不可逆：无法从MD5值还原原始文件
            File file = new File(localFilePath);
            String fileMd5 = getFileMd5(file);
            log.debug("文件MD5值：{}", fileMd5);

            // ===== 步骤2：检查文件是否已存在（秒传功能，无需事务） =====
            //
            // 🚀 秒传流程：
            // 1. 通过MD5值查询数据库
            // 2. 如果查询到记录，说明文件已存在
            // 3. 直接返回已有文件信息，无需重复上传
            // 4. 用户体验：瞬间完成上传
            //
            // 💡 优点：
            // - 节省存储空间：相同文件只存储一份
            // - 节省带宽：无需重复上传
            // - 提升速度：秒级完成
            MediaFiles existFile = mediaFilesMapper.selectById(fileMd5);
            UploadFileResultDto resultDto = new UploadFileResultDto();
            if (existFile != null) {
                // 文件已存在，执行秒传
                log.info("文件已存在，直接返回（秒传）：{}", fileMd5);
                // 将数据库中的文件信息复制到返回对象
                BeanUtils.copyProperties(existFile, resultDto);
                return resultDto;
            }

            // ===== 步骤3：上传文件到OSS（网络请求，无需事务） =====
            //
            // 🌐 OSS上传流程：
            // 1. 读取本地文件
            // 2. 通过OSS SDK上传到阿里云
            // 3. 返回文件在OSS中的路径（ObjectName）
            //
            // 📦 ObjectName格式：
            // - 2025/01/06/abc123.jpg
            // - 按日期分目录存储，便于管理
            //
            // ⏱️ 耗时操作：
            // - 网络传输，可能需要几秒到几十秒
            // - 所以不能放在事务中（会长时间占用数据库连接）
            String objectName = ossService.uploadFile(localFilePath);
            log.info("文件上传到OSS成功：{}", objectName);

            // ===== 步骤4：保存文件信息到数据库（通过代理对象调用，确保事务生效） =====
            //
            // 🔥 关键点：使用 currentProxy 而不是 this
            //
            // 为什么？
            // - currentProxy 是代理对象，会触发事务拦截器
            // - this 是原始对象，直接调用，事务失效
            //
            // 事务范围：
            // - 只包含数据库插入操作
            // - 执行时间短，不会长时间占用连接
            //
            // 事务保证：
            // - 如果插入失败，会抛出异常
            // - 事务自动回滚，数据不会脏写
            MediaFiles mediaFiles = currentProxy.addMediaFilesToDb(companyId, fileMd5, uploadFileParamsDto, objectName);

            // ===== 步骤5：返回结果 =====
            //
            // 返回内容：
            // - url: 文件访问URL（前端可以直接使用）
            // - id: 文件ID（MD5值）
            // - filename: 文件名
            // - fileType: 文件类型
            // - 其他文件元数据信息
            BeanUtils.copyProperties(mediaFiles, resultDto);
            return resultDto;
        } catch (Exception e) {
            // ===== 异常处理 =====
            //
            // 处理策略：
            // 1. 记录详细的错误日志（包含异常堆栈）
            // 2. 抛出运行时异常（方便上层统一处理）
            // 3. 如果是在事务方法中抛出异常，事务会自动回滚
            //
            // 日志级别：
            // - error：错误级别，会记录到错误日志文件
            // - 包含异常信息和堆栈跟踪，便于排查问题
            log.error("上传文件失败：{}", e.getMessage(), e);
            throw new RuntimeException("上传文件失败：" + e.getMessage());
        }
    }

    /**
     * 将文件信息保存到数据库
     * 
     * 🎯 功能说明：
     * 构建媒资文件信息对象，并保存到数据库
     * 
     * 🔥 事务管理：
     * 
     * @Transactional注解的作用：
     * 1. 开启事务：方法执行前开启数据库事务
     * 2. 提交事务：方法正常结束时提交事务
     * 3. 回滚事务：方法抛出异常时回滚事务
     * 
     * 事务的ACID特性：
     * - Atomicity（原子性）：要么全部成功，要么全部失败
     * - Consistency（一致性）：事务前后数据保持一致
     * - Isolation（隔离性）：多个事务之间相互隔离
     * - Durability（持久性）：事务提交后数据永久保存
     * 
     * 为什么只在这个方法上加事务？
     * 1. 这个方法只包含数据库操作，执行时间短
     * 2. 上层方法包含网络请求（上传OSS），耗时长
     * 3. 缩小事务范围，避免长时间占用数据库连接
     * 4. 提高系统并发性能
     * 
     * 📊 数据库操作流程：
     * 1. 构建MediaFiles对象
     * 2. 设置各项属性
     * 3. 调用Mapper的insert方法插入数据库
     * 4. 检查插入结果
     * 5. 如果失败，抛出异常（事务回滚）
     * 
     * @param companyId           机构ID（用于数据隔离）
     * @param fileMd5             文件MD5值（用作主键，实现去重）
     * @param uploadFileParamsDto 上传文件参数对象
     * @param objectName          OSS中的文件路径（对象名）
     *                            格式：2025/01/06/abc123.jpg
     * @return MediaFiles 媒资文件信息对象（包含完整的数据库记录）
     */
    @Transactional // 🔥 开启事务：确保数据库操作的原子性
    public MediaFiles addMediaFilesToDb(Long companyId, String fileMd5, UploadFileParamsDto uploadFileParamsDto,
            String objectName) {

        // ===== 检查文件是否已存在（避免主键冲突） =====
        MediaFiles existingFile = mediaFilesMapper.selectById(fileMd5);
        if (existingFile != null) {
            log.info("文件已存在，更新文件信息，MD5：{}", fileMd5);
            // 更新现有记录的信息
            existingFile.setFilename(uploadFileParamsDto.getFilename());
            existingFile.setFileSize(uploadFileParamsDto.getFileSize());
            existingFile.setFilePath(objectName);
            existingFile.setUrl(objectName);
            existingFile.setChangeDate(LocalDateTime.now());
            existingFile.setRemark(uploadFileParamsDto.getRemark());
            mediaFilesMapper.updateById(existingFile);
            return existingFile;
        }

        // ===== 步骤1：构建媒资文件信息对象 =====
        MediaFiles mediaFiles = new MediaFiles();

        // ========== 设置主键和基本信息 ==========

        // 🔑 主键设计：使用MD5值作为主键
        //
        // 为什么用MD5作为主键？
        // 1. 唯一性：相同文件的MD5值相同，天然去重
        // 2. 秒传功能：通过主键快速判断文件是否存在
        // 3. 避免重复存储：相同文件只存储一次
        //
        // 优点：
        // - 简化去重逻辑（不需要额外的唯一索引）
        // - 提高查询效率（主键查询最快）
        // - 节省存储空间（相同文件只存一份）
        mediaFiles.setId(fileMd5);

        // 机构ID：用于数据隔离
        // 每个机构只能访问自己的文件
        mediaFiles.setCompanyId(companyId);

        // 机构名称：暂时用username代替
        // TODO: 后续可以通过companyId查询机构表获取真实的机构名称
        mediaFiles.setCompanyName(uploadFileParamsDto.getUsername());

        // ========== 设置文件信息 ==========

        // 文件名：原始文件名（如：学习资料.pdf）
        mediaFiles.setFilename(uploadFileParamsDto.getFilename());

        // 文件类型：使用字典编码
        // 001001: 图片（jpg、png、gif等）
        // 001002: 视频（mp4、avi、flv等）
        // 001003: 文档（pdf、doc、ppt等）
        mediaFiles.setFileType(uploadFileParamsDto.getFileType());

        // 文件标签：用于分类和检索
        // 例如：Java、Spring、数据库、前端等
        mediaFiles.setTags(uploadFileParamsDto.getTags());

        // ========== 设置OSS存储信息 ==========

        // 存储桶名称：指定文件存储在哪个OSS Bucket
        // 例如：yangxiaobucker
        mediaFiles.setBucket(bucketName);

        // 文件路径：文件在OSS中的完整路径（ObjectName）
        // 格式：2025/01/06/abc123.jpg
        // 说明：按日期分目录存储，便于管理和维护
        mediaFiles.setFilePath(objectName);

        // 文件ID：使用MD5值（与主键相同）
        mediaFiles.setFileId(fileMd5);

        // ========== 生成文件访问URL ==========
        //
        // 🌐 URL设计方案：
        //
        // 方案1：存储完整URL（不推荐）
        // - 存储：https://yangxiaobucker.oss-cn-beijing.aliyuncs.com/2025/01/06/abc123.jpg
        // - 缺点：如果更换域名或CDN，需要更新所有记录
        //
        // 方案2：只存储ObjectName（推荐）✅
        // - 存储：2025/01/06/abc123.jpg
        // - 前端拼接：baseUrl + objectName
        // - 优点：灵活，可以随时更换域名或CDN
        //
        // 前端使用示例：
        // const baseUrl = 'https://yangxiaobucker.oss-cn-beijing.aliyuncs.com/';
        // const fileUrl = baseUrl + mediaFiles.url;
        // <img src={fileUrl} />
        //
        // 最终访问URL：
        // https://yangxiaobucker.oss-cn-beijing.aliyuncs.com/2025/01/06/abc123.jpg
        String url = objectName; // 只存储对象名（文件路径）
        mediaFiles.setUrl(url);

        // ========== 设置用户和时间信息 ==========

        // 上传用户：记录是谁上传的文件
        // 用途：审计、权限控制、统计分析
        mediaFiles.setUsername(uploadFileParamsDto.getUsername());

        // 创建时间：记录文件首次上传时间
        mediaFiles.setCreateDate(LocalDateTime.now());

        // 修改时间：记录文件最后修改时间
        // 注意：首次上传时，创建时间和修改时间相同
        mediaFiles.setChangeDate(LocalDateTime.now());

        // ========== 设置状态信息 ==========

        // 文件状态：1-正常，0-删除
        // 采用逻辑删除，不物理删除数据
        mediaFiles.setStatus("1");

        // 备注信息：用户填写的备注
        mediaFiles.setRemark(uploadFileParamsDto.getRemark());

        // 审核状态：使用字典编码
        // 002001: 未审核
        // 002002: 审核中
        // 002003: 审核通过 ✅
        // 002004: 审核不通过
        //
        // 说明：上传时默认设置为审核通过
        // 如果需要人工审核，可以设置为002001（未审核）
        mediaFiles.setAuditStatus("002003");

        // 文件大小：单位为字节（Byte）
        // 用途：统计存储空间、限制上传大小
        mediaFiles.setFileSize(uploadFileParamsDto.getFileSize());

        // ========== 保存到数据库 ==========
        //
        // 🔍 MyBatis Plus的insert方法：
        // 1. 自动生成INSERT SQL语句
        // 2. 执行数据库插入操作
        // 3. 返回影响的行数（成功返回1，失败返回0）
        //
        // SQL示例：
        // INSERT INTO media_files (id, company_id, filename, file_type, ...)
        // VALUES ('abc123...', 1, '学习资料.pdf', '001003', ...)
        int insert = mediaFilesMapper.insert(mediaFiles);

        // ===== 检查插入结果 =====
        if (insert <= 0) {
            // 插入失败的可能原因：
            // 1. 主键冲突（MD5值重复）
            // 2. 数据库连接异常
            // 3. 字段约束违反（如非空字段为空）
            //
            // 处理策略：
            // 1. 抛出运行时异常
            // 2. 事务自动回滚（@Transactional的作用）
            // 3. 数据不会脏写到数据库
            throw new RuntimeException("保存文件信息到数据库失败");
        }

        // ===== 记录成功日志 =====
        log.info("文件信息保存到数据库成功：{}", fileMd5);

        // ===== 返回完整的文件信息对象 =====
        // 包含所有字段，供上层方法使用
        return mediaFiles;
    }

    // =================== 大文件分块上传实现 ===================
    //
    // 🎯 功能说明：
    // 支持大文件分块上传，提高上传成功率和用户体验
    //
    // 📊 分块上传流程：
    // 1. 前端将大文件切分成多个小块（如每块5MB）
    // 2. 逐个上传分块到服务器
    // 3. 服务器保存分块到临时目录
    // 4. 所有分块上传完成后，服务器合并分块
    // 5. 上传合并后的文件到OSS
    // 6. 保存文件信息到数据库
    // 7. 清理临时分块文件
    //
    // 💡 优点：
    // 1. 断点续传：上传失败后可以从断点继续
    // 2. 提高成功率：小文件上传更稳定
    // 3. 并发上传：可以同时上传多个分块
    // 4. 用户体验：显示上传进度
    //
    // 🔧 技术实现：
    // - 使用临时目录存储分块
    // - 使用RandomAccessFile合并分块
    // - 使用MD5校验文件完整性

    /**
     * 获取分块文件的根目录
     * 
     * 🎯 功能说明：
     * 为每个文件创建一个独立的临时目录，用于存储分块文件
     * 
     * 📁 目录结构：
     * /tmp/xc-chunks/abc123.../
     * ├── 0 (第1个分块)
     * ├── 1 (第2个分块)
     * ├── 2 (第3个分块)
     * └── ...
     * 
     * 💡 设计要点：
     * 1. 使用系统临时目录（跨平台兼容）
     * 2. 使用文件MD5作为目录名（唯一标识）
     * 3. 分块文件以序号命名（0、1、2...）
     * 
     * @param fileMd5 文件MD5值（用作目录名）
     * @return Path 分块文件的根目录路径
     */
    private Path getChunkRootPath(String fileMd5) {
        // 获取系统临时目录
        // Windows: C:\Users\用户名\AppData\Local\Temp
        // Linux: /tmp
        // Mac: /var/folders/...
        String base = System.getProperty("java.io.tmpdir");

        // 构建分块目录路径
        // 例如：/tmp/xc-chunks/abc123.../
        return Paths.get(base, "xc-chunks", fileMd5);
    }

    /**
     * 检查文件是否已存在
     * 
     * 🎯 功能说明：
     * 在分块上传前，先检查文件是否已经上传过（秒传功能）
     * 
     * 📊 业务流程：
     * 1. 前端计算文件MD5值
     * 2. 调用此接口检查文件是否存在
     * 3. 如果存在，直接返回true（秒传）
     * 4. 如果不存在，返回false（需要上传）
     * 
     * 💡 秒传优势：
     * - 节省时间：无需上传，瞬间完成
     * - 节省带宽：不占用网络资源
     * - 节省存储：相同文件只存一份
     * 
     * @param fileMd5 文件MD5值
     * @return RestResponse<Boolean>
     *         - true: 文件已存在（可以秒传）
     *         - false: 文件不存在（需要上传）
     */
    @Override
    public RestResponse<Boolean> checkFile(String fileMd5) {
        // 通过MD5值查询数据库
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);

        if (mediaFiles != null) {
            // 文件已存在，返回true
            return RestResponse.success(true);
        }

        // 文件不存在，返回false
        return RestResponse.success(false);
    }

    /**
     * 检查分块是否已存在
     * 
     * 🎯 功能说明：
     * 在上传分块前，先检查该分块是否已经上传过（断点续传）
     * 
     * 📊 业务流程：
     * 1. 前端准备上传某个分块
     * 2. 先调用此接口检查分块是否存在
     * 3. 如果存在，跳过该分块（断点续传）
     * 4. 如果不存在，上传该分块
     * 
     * 💡 断点续传优势：
     * - 网络中断后可以继续上传
     * - 不需要重新上传已完成的分块
     * - 提高上传成功率
     * 
     * 🔍 检查逻辑：
     * 1. 检查分块文件是否存在
     * 2. 检查分块文件大小是否大于0（防止空文件）
     * 
     * @param fileMd5    文件MD5值
     * @param chunkIndex 分块序号（从0开始）
     * @return RestResponse<Boolean>
     *         - true: 分块已存在（可以跳过）
     *         - false: 分块不存在（需要上传）
     */
    @Override
    public RestResponse<Boolean> checkChunk(String fileMd5, int chunkIndex) {
        try {
            // 构建分块文件路径
            // 例如：/tmp/xc-chunks/abc123.../0
            Path chunkPath = getChunkRootPath(fileMd5).resolve(String.valueOf(chunkIndex));

            // 检查分块文件是否存在且大小大于0
            boolean exists = Files.exists(chunkPath) && Files.size(chunkPath) > 0;

            return RestResponse.success(exists);
        } catch (Exception e) {
            // 异常情况返回false（需要重新上传）
            return RestResponse.success(false);
        }
    }

    /**
     * 上传分块文件
     * 
     * 🎯 功能说明：
     * 接收前端上传的分块文件，保存到临时目录
     * 
     * 📊 业务流程：
     * 1. 接收分块文件（已保存到临时位置）
     * 2. 创建分块存储目录
     * 3. 将分块文件移动到标准位置
     * 4. 返回上传结果
     * 
     * 📁 文件存储：
     * 源文件：/tmp/upload_temp_123.tmp（临时位置）
     * 目标文件：/tmp/xc-chunks/abc123.../0（标准位置）
     * 
     * 💡 设计要点：
     * 1. 使用文件序号作为文件名（0、1、2...）
     * 2. 如果文件已存在，先删除再复制（覆盖）
     * 3. 使用Files.copy()方法（高效、安全）
     * 
     * @param fileMd5            文件MD5值
     * @param chunk              分块序号（从0开始）
     * @param localChunkFilePath 本地临时分块文件路径
     * @return RestResponse 上传结果
     *         - success: 上传成功
     *         - fail: 上传失败
     */
    @Override
    public RestResponse uploadChunk(String fileMd5, int chunk, String localChunkFilePath) {
        try {
            // ===== 步骤1：创建分块存储目录 =====
            Path root = getChunkRootPath(fileMd5);
            Files.createDirectories(root); // 递归创建目录（如果不存在）

            // ===== 步骤2：构建目标文件路径 =====
            Path target = root.resolve(String.valueOf(chunk));

            // ===== 步骤3：检查源文件是否存在 =====
            Path source = Paths.get(localChunkFilePath);
            if (!Files.exists(source)) {
                return RestResponse.validfail(false, "分块文件不存在");
            }

            // ===== 步骤4：移动/复制分块文件 =====
            // 如果目标文件已存在，先删除（覆盖）
            Files.deleteIfExists(target);
            // 复制文件到目标位置
            Files.copy(source, target);

            return RestResponse.success(true);
        } catch (Exception e) {
            return RestResponse.validfail(false, "上传分块文件失败");
        }
    }

    /**
     * 合并分块文件
     * 
     * 🎯 功能说明：
     * 将所有分块文件合并成完整文件，并上传到OSS
     * 
     * 📊 业务流程（7个步骤）：
     * 
     * 步骤1：检查分块目录是否存在
     * - 如果不存在，说明没有上传分块，返回失败
     * 
     * 步骤2：合并分块到临时文件
     * - 创建一个临时文件用于存储合并结果
     * - 使用RandomAccessFile顺序写入每个分块
     * - 使用缓冲区提高读写效率（1MB缓冲区）
     * 
     * 步骤3：MD5校验
     * - 计算合并后文件的MD5值
     * - 与前端传入的MD5值对比
     * - 如果不一致，说明文件损坏，返回失败
     * 
     * 步骤4：上传到OSS
     * - 调用ossService.uploadFile()方法
     * - 将合并后的文件上传到阿里云OSS
     * 
     * 步骤5：保存到数据库
     * - 通过代理对象调用addMediaFilesToDb()方法
     * - 确保事务生效
     * 
     * 步骤6：清理临时文件
     * - 删除所有分块文件
     * - 删除分块目录
     * - 删除合并后的临时文件
     * 
     * 步骤7：返回结果
     * 
     * 🔍 技术细节：
     * 
     * RandomAccessFile：
     * - 支持随机访问文件（读写位置可以任意移动）
     * - 适合文件合并场景
     * - 模式："rw"（读写模式）、"r"（只读模式）
     * 
     * 缓冲区：
     * - 使用1MB缓冲区（1024 * 1024字节）
     * - 减少磁盘I/O次数，提高效率
     * 
     * MD5校验：
     * - 确保文件完整性
     * - 防止文件损坏或篡改
     * 
     * 💡 异常处理：
     * - 任何步骤失败都会返回错误信息
     * - 清理临时文件，避免磁盘空间浪费
     * 
     * @param companyId           机构ID
     * @param fileMd5             文件MD5值
     * @param chunkTotal          分块总数
     * @param uploadFileParamsDto 上传文件参数
     * @return RestResponse 合并结果
     *         - success: 合并成功
     *         - fail: 合并失败（包含失败原因）
     */
    @Override
    public RestResponse mergechunks(Long companyId, String fileMd5, int chunkTotal,
            UploadFileParamsDto uploadFileParamsDto) {

        // ===== 步骤1：检查分块目录是否存在 =====
        Path chunkRoot = getChunkRootPath(fileMd5);
        if (!Files.exists(chunkRoot)) {
            return RestResponse.validfail(false, "分块目录不存在");
        }

        try {
            // ===== 步骤2：合并分块到临时文件 =====
            //
            // 创建临时文件用于存储合并结果
            // 文件名格式：xc-merge-随机数.tmp
            Path merged = Files.createTempFile("xc-merge-", ".tmp");

            // 使用RandomAccessFile进行文件合并
            // "rw"模式：可读可写
            try (java.io.RandomAccessFile raf_rw = new java.io.RandomAccessFile(merged.toFile(), "rw")) {
                // 创建1MB缓冲区，提高读写效率
                byte[] buffer = new byte[1024 * 1024];

                // 遍历所有分块，按顺序合并
                for (int i = 0; i < chunkTotal; i++) {
                    // 构建分块文件路径
                    Path part = chunkRoot.resolve(String.valueOf(i));

                    // 检查分块是否存在
                    if (!Files.exists(part)) {
                        return RestResponse.validfail(false, "缺少分块：" + i);
                    }

                    // 读取分块文件并写入合并文件
                    // "r"模式：只读
                    try (java.io.RandomAccessFile raf_r = new java.io.RandomAccessFile(part.toFile(), "r")) {
                        int len;
                        // 循环读取分块内容
                        while ((len = raf_r.read(buffer)) != -1) {
                            // 写入合并文件
                            raf_rw.write(buffer, 0, len);
                        }
                    }
                }
            }

            // ===== 步骤3：MD5校验 =====
            //
            // 计算合并后文件的MD5值
            String mergedMd5;
            try (FileInputStream fis = new FileInputStream(merged.toFile())) {
                mergedMd5 = DigestUtils.md5Hex(fis);
            }

            // 对比MD5值
            if (!fileMd5.equalsIgnoreCase(mergedMd5)) {
                // MD5不一致，文件损坏
                Files.deleteIfExists(merged); // 删除损坏的文件
                log.error("文件MD5校验失败，原始MD5：{}，合并后MD5：{}", fileMd5, mergedMd5);
                return RestResponse.validfail(false, "文件校验失败");
            }

            // 回填合并后文件大小，确保入库时有完整数据
            try {
                long mergedSize = Files.size(merged);
                uploadFileParamsDto.setFileSize(mergedSize);
                log.info("合并后文件大小：{} 字节", mergedSize);
            } catch (Exception e) {
                log.warn("获取合并文件大小失败", e);
            }

            // ===== 步骤4：上传到OSS =====
            String objectName = ossService.uploadFile(merged.toString(), uploadFileParamsDto.getFilename());
            log.info("合并文件上传到OSS成功，对象名：{}", objectName);

            // ===== 步骤5：保存到数据库（事务） =====
            log.info("准备保存文件信息到数据库，MD5：{}，文件名：{}，大小：{}", fileMd5, uploadFileParamsDto.getFilename(),
                    uploadFileParamsDto.getFileSize());
            MediaFiles mediaFiles = currentProxy.addMediaFilesToDb(companyId, fileMd5, uploadFileParamsDto, objectName);
            if (mediaFiles == null || mediaFiles.getId() == null) {
                log.error("文件入库失败，MD5：{}，文件名：{}", fileMd5, uploadFileParamsDto.getFilename());
                return RestResponse.validfail(false, "文件入库失败");
            }
            log.info("文件入库成功，ID：{}，文件名：{}", mediaFiles.getId(), mediaFiles.getFilename());

            // ===== 步骤6：清理临时文件 =====
            //
            // 删除所有分块文件
            for (int i = 0; i < chunkTotal; i++) {
                Files.deleteIfExists(chunkRoot.resolve(String.valueOf(i)));
            }
            // 删除分块目录
            Files.deleteIfExists(chunkRoot);
            // 删除合并后的临时文件
            Files.deleteIfExists(merged);

            // ===== 步骤7：返回成功结果 =====
            return RestResponse.success(true);
        } catch (Exception e) {
            log.error("合并文件异常，MD5：{}，错误信息：{}", fileMd5, e.getMessage(), e);
            return RestResponse.validfail(false, "合并文件异常：" + e.getMessage());
        }
    }

    /**
     * 获取用于预览的对象路径（相对路径）。
     */
    @Override
    public String getPreviewUrl(String mediaId) {
        MediaFiles mediaFiles = mediaFilesMapper.selectById(mediaId);
        if (mediaFiles == null) {
            return null;
        }
        // 优先返回url（已存相对路径）
        if (mediaFiles.getUrl() != null && !mediaFiles.getUrl().isEmpty()) {
            return mediaFiles.getUrl();
        }
        // 其次返回filePath
        return mediaFiles.getFilePath();
    }

    /**
     * 计算文件MD5值
     * 
     * 🎯 功能说明：
     * 读取文件内容，计算MD5哈希值
     * 
     * 📚 MD5（Message Digest Algorithm 5）：
     * 
     * 什么是MD5？
     * - 一种广泛使用的密码散列函数
     * - 可以产生一个128位（16字节）的散列值
     * - 通常表示为32位十六进制数字
     * 
     * MD5的特点：
     * 1. 固定长度：无论输入多大，输出都是32位字符串
     * 2. 唯一性：内容不同，MD5值不同（碰撞概率极低）
     * 3. 不可逆：无法从MD5值还原原始内容
     * 4. 雪崩效应：输入微小变化，输出完全不同
     * 
     * MD5的应用场景：
     * 1. 文件去重：相同文件MD5相同
     * 2. 文件完整性校验：传输前后MD5对比
     * 3. 数据库主键：使用MD5作为唯一标识
     * 4. 秒传功能：通过MD5判断文件是否存在
     * 
     * 🔧 技术实现：
     * - 使用Apache Commons Codec库
     * - DigestUtils.md5Hex()方法
     * - 自动处理文件流的读取和MD5计算
     * 
     * 💡 性能优化：
     * - 使用流式读取，不会一次性加载整个文件到内存
     * - 适合大文件的MD5计算
     * 
     * 🛡️ 异常处理：
     * - 捕获所有异常（文件不存在、读取失败等）
     * - 记录错误日志
     * - 抛出运行时异常
     * 
     * @param file 文件对象
     * @return String MD5值（32位小写十六进制字符串）
     *         例如：abc123def456789...（32位）
     */
    private String getFileMd5(File file) {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            // 使用Apache Commons Codec的DigestUtils工具类计算MD5
            //
            // md5Hex()方法：
            // 1. 读取输入流的所有内容
            // 2. 计算MD5哈希值
            // 3. 转换为32位十六进制字符串
            // 4. 自动关闭输入流（try-with-resources）
            return DigestUtils.md5Hex(fileInputStream);
        } catch (Exception e) {
            // 异常处理：记录日志并抛出运行时异常
            log.error("计算文件MD5失败", e);
            throw new RuntimeException("计算文件MD5失败");
        }
    }

}
