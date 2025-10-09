# XXL-JOB 分布式任务调度集成完成总结

## ✅ 已完成的工作

### 一、核心概念讲解

#### 1.1 任务调度 vs 分布式任务调度

**任务调度**：

- 定义：在固定时间点或时间间隔自动执行任务
- 实现方式：Thread.sleep、Timer、ScheduledExecutorService、Spring @Scheduled、Quartz
- 适用场景：单机任务，如数据备份、定时统计

**分布式任务调度**：

- 定义：多台服务器协同执行任务，提高处理能力
- 核心目标：并行执行、高可用、弹性扩容、避免重复、任务监控
- 适用场景：大量任务需要高效处理，如视频转码、批量数据处理

#### 1.2 XXL-JOB 架构原理

```
工作流程：
1. 执行器启动 → 自动注册到调度中心
2. 调度中心 → 根据Cron表达式触发任务
3. 调度中心 → 下发任务到执行器
4. 执行器 → 线程池执行任务
5. 执行器 → 异步上报结果到调度中心
6. 调度中心 → 记录执行日志
```

**优势**：

- ✅ 轻量级，易上手
- ✅ Web 管理界面
- ✅ 支持任务分片
- ✅ 故障自动转移
- ✅ 动态修改配置

---

### 二、已创建的文件

#### 2.1 核心代码文件

1. **依赖配置**

   - 文件：`xuecheng-plus-media/xuecheng-plus-media-service/pom.xml`
   - 内容：添加 `xxl-job-core:2.3.1` 依赖

2. **执行器配置类**

   - 文件：`xuecheng-plus-media/xuecheng-plus-media-service/src/main/java/com/xuecheng/media/config/XxlJobConfig.java`
   - 功能：读取 Nacos 配置，初始化 XXL-JOB 执行器

3. **任务处理器**
   - 文件：`xuecheng-plus-media/xuecheng-plus-media-service/src/main/java/com/xuecheng/media/job/MediaProcessJob.java`
   - 功能：定义各种任务处理方法
   - 任务列表：
     - `testJob` - 测试任务
     - `chunkCleanupJob` - 分块文件清理
     - `videoTranscodeJob` - 视频转码（待实现）
     - `paramJob` - 参数示例任务

#### 2.2 配置文件

1. **数据库初始化脚本**

   - 文件：`xxl-job-init.sql`
   - 功能：创建 `xxl_job` 数据库和 8 张表
   - 表说明：
     - xxl_job_info - 任务信息
     - xxl_job_log - 调度日志
     - xxl_job_group - 执行器分组
     - xxl_job_registry - 执行器注册
     - 等...

2. **Nacos 配置示例**
   - 文件：`nacos-config-xxljob.yaml`
   - 配置项：
     - 调度中心地址
     - 执行器 AppName
     - 执行器端口
     - 日志路径

#### 2.3 文档文件

1. **详细集成文档**

   - 文件：`XXL-JOB集成指南.md`
   - 内容：完整的集成步骤、配置说明、常见问题

2. **快速启动指南**

   - 文件：`XXL-JOB快速启动指南.md`
   - 内容：5 分钟快速上手教程

3. **完成总结**
   - 文件：`XXL-JOB集成完成总结.md`（本文档）
   - 内容：工作总结和后续计划

---

### 三、集成步骤回顾

#### 步骤 1：调度中心搭建 ✅

```bash
# 1. 创建数据库
mysql -uroot -p < xxl-job-init.sql

# 2. 下载XXL-JOB（2.3.1版本）
# 从 GitHub 或码云下载

# 3. 修改配置
# 文件：xxl-job-admin/src/main/resources/application.properties
# 修改数据库连接、端口等

# 4. 启动调度中心
# IDEA运行 XxlJobAdminApplication
# 或命令行：java -jar xxl-job-admin-2.3.1.jar

# 5. 访问界面
http://localhost:8888/xxl-job-admin
账号：admin / 123456
```

#### 步骤 2：执行器集成 ✅

```xml
<!-- 1. 添加依赖 -->
<dependency>
    <groupId>com.xuxueli</groupId>
    <artifactId>xxl-job-core</artifactId>
    <version>2.3.1</version>
</dependency>
```

```yaml
# 2. 添加Nacos配置
xxl:
  job:
    admin:
      addresses: http://127.0.0.1:8888/xxl-job-admin
    executor:
      appname: media-service-executor
      port: 9999
      logpath: D:/project/java/xuecheng-plus-project/logs/xxl-job
      logretentiondays: 30
```

```java
// 3. 创建配置类
@Configuration
public class XxlJobConfig {
    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {
        // 初始化执行器
    }
}
```

```java
// 4. 创建任务处理器
@Component
public class MediaProcessJob {
    @XxlJob("testJob")
    public void testJob() {
        // 任务逻辑
    }
}
```

#### 步骤 3：调度中心配置 ✅

```
1. 登录调度中心
2. 执行器管理 → 新增
   - AppName: media-service-executor
   - 名称: 媒资服务执行器
   - 注册方式: 自动注册

3. 任务管理 → 新增
   - 执行器: 媒资服务执行器
   - 任务描述: 测试任务
   - Cron: 0/10 * * * * ?
   - JobHandler: testJob

4. 启动任务
5. 查看调度日志
```

---

### 四、功能验证清单

#### 4.1 执行器验证

- [x] 执行器成功注册到调度中心
- [x] 调度中心显示在线机器
- [x] 启动日志显示 XXL-JOB 配置信息
- [x] 执行器端口正常监听

#### 4.2 任务验证

- [x] 任务成功创建
- [x] 任务按 Cron 表达式执行
- [x] 任务执行日志正常记录
- [x] 任务执行结果正常回调

#### 4.3 功能验证

- [x] 测试任务（testJob）正常执行
- [x] 分块清理任务（chunkCleanupJob）集成完成
- [x] 视频转码任务（videoTranscodeJob）框架搭建
- [x] 参数任务（paramJob）示例完成

---

### 五、当前任务配置

#### 5.1 已实现的任务

| 任务名   | JobHandler        | Cron 表达式        | 说明             | 状态      |
| -------- | ----------------- | ------------------ | ---------------- | --------- |
| 测试任务 | testJob           | 0/10 \* \* \* \* ? | 每 10 秒执行一次 | ✅ 可用   |
| 分块清理 | chunkCleanupJob   | 0 0 2 \* \* ?      | 每天凌晨 2 点    | ✅ 可用   |
| 视频转码 | videoTranscodeJob | 0 0/5 \* \* \* ?   | 每 5 分钟        | ⏳ 待实现 |
| 参数示例 | paramJob          | -                  | 手动触发         | ✅ 可用   |

#### 5.2 任务详细说明

**1. 测试任务（testJob）**

- 功能：测试 XXL-JOB 是否正常工作
- 执行逻辑：打印日志、Sleep 2 秒、返回成功
- 建议配置：
  - Cron: `0/10 * * * * ?` （每 10 秒）
  - 路由策略：第一个
  - 阻塞策略：单机串行

**2. 分块清理任务（chunkCleanupJob）**

- 功能：清理 24 小时前的分块文件
- 执行逻辑：
  1. 调用 `ChunkCleanupTask.cleanupOldChunks()`
  2. 获取清理统计信息
  3. 记录日志并上报结果
- 建议配置：
  - Cron: `0 0 2 * * ?` （每天凌晨 2 点）
  - 路由策略：第一个
  - 阻塞策略：单机串行

**3. 视频转码任务（videoTranscodeJob）**

- 功能：批量转码待处理视频
- 执行逻辑（待实现）：
  1. 获取分片参数
  2. 查询待转码视频（按分片）
  3. 调用 FFmpeg 转码
  4. 更新视频状态
- 建议配置：
  - Cron: `0 0/5 * * * ?` （每 5 分钟）
  - 路由策略：分片广播（支持分布式）
  - 阻塞策略：单机串行

**4. 参数示例任务（paramJob）**

- 功能：演示任务参数的使用
- 执行逻辑：
  1. 获取任务参数
  2. 根据参数执行不同逻辑
  3. 返回执行结果
- 使用场景：需要动态配置的任务

---

### 六、Cron 表达式参考

#### 6.1 常用表达式

```
格式：秒 分 时 日 月 周

每10秒：         0/10 * * * * ?
每分钟：         0 * * * * ?
每5分钟：        0 0/5 * * * ?
每小时：         0 0 * * * ?
每天0点：        0 0 0 * * ?
每天2点：        0 0 2 * * ?
每天12点：       0 0 12 * * ?
每周一8点：      0 0 8 ? * MON
每月1号0点：     0 0 0 1 * ?
工作日10:15：    0 15 10 ? * MON-FRI
```

#### 6.2 在线工具

- **Cron 表达式生成器**：https://cron.qqe2.com/
- **使用方法**：
  1. 访问网站
  2. 选择时间规则
  3. 自动生成表达式
  4. 查看执行时间预览

---

### 七、路由策略说明

| 策略            | 适用场景       | 说明                         |
| --------------- | -------------- | ---------------------------- |
| 第一个          | 单机任务       | 固定第一台机器执行           |
| 最后一个        | 单机任务       | 固定最后一台机器执行         |
| 轮询            | 负载均衡       | 依次轮询所有机器             |
| 随机            | 负载均衡       | 随机选择机器                 |
| 一致性 HASH     | 状态保持       | 相同参数路由到同一机器       |
| 最不经常使用    | 负载均衡       | 选择使用频率最低的机器       |
| 最近最久未使用  | 负载均衡       | 选择最久未使用的机器         |
| 故障转移        | 高可用         | 按顺序测试，第一个可用的执行 |
| 忙碌转移        | 高可用         | 第一个忙则转移到下一个       |
| **分片广播** ⭐ | **分布式任务** | **所有机器同时执行不同分片** |

**推荐配置**：

- 单机任务：第一个
- 负载均衡：轮询
- 分布式大任务：**分片广播**

---

### 八、分片任务详解

#### 8.1 什么是分片任务？

**问题场景**：

```
需求：转码10000个视频
单机方案：1台服务器处理10000个视频 → 耗时太长 ❌

分片方案：3台服务器，每台处理不同的视频
- 服务器1：处理视频 0, 3, 6, 9, ... (分片0)
- 服务器2：处理视频 1, 4, 7, 10, ... (分片1)
- 服务器3：处理视频 2, 5, 8, 11, ... (分片2)

效率提升3倍 ✅
```

#### 8.2 分片参数使用

```java
@XxlJob("videoTranscodeJob")
public void videoTranscodeJob() {
    // 获取分片参数
    int shardIndex = XxlJobHelper.getShardIndex();  // 当前分片索引（0,1,2）
    int shardTotal = XxlJobHelper.getShardTotal();  // 总分片数（3）

    // 根据分片查询数据
    List<Video> videos = videoService.getByShardIndex(shardIndex, shardTotal);

    // 执行转码
    for (Video video : videos) {
        transcodeVideo(video);
    }
}
```

**SQL 实现**：

```sql
-- 分片查询（模运算）
SELECT * FROM media_files
WHERE MOD(id, #{shardTotal}) = #{shardIndex}
  AND status = 'pending'
ORDER BY create_date ASC
LIMIT 100;
```

#### 8.3 分片任务配置

**调度中心配置**：

```
1. 路由策略：选择 "分片广播"
2. 启动任务
3. 所有在线执行器自动分片执行
```

**效果**：

- 3 台机器在线 → 自动分为 3 片
- 5 台机器在线 → 自动分为 5 片
- 动态扩容无需修改配置

---

### 九、监控告警（待配置）

#### 9.1 邮件告警

**配置调度中心**：

```properties
# application.properties
spring.mail.host=smtp.qq.com
spring.mail.port=25
spring.mail.username=your_email@qq.com
spring.mail.password=your_password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
```

**配置任务告警邮箱**：

```
任务管理 → 编辑任务 → 报警邮件
填写：admin@example.com
```

#### 9.2 告警规则

- ✅ 任务执行失败
- ✅ 任务执行超时
- ✅ 执行器下线

---

### 十、对比：@Scheduled vs XXL-JOB

#### 10.1 迁移方案

**原有代码**（Spring @Scheduled）：

```java
@Component
public class ChunkCleanupTask {

    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOldChunks() {
        // 清理逻辑
    }
}
```

**迁移后**（XXL-JOB）：

```java
@Component
public class MediaProcessJob {

    @Autowired
    private ChunkCleanupTask chunkCleanupTask;

    @XxlJob("chunkCleanupJob")
    public void chunkCleanupJob() {
        chunkCleanupTask.cleanupOldChunks();  // 复用原有逻辑
    }
}
```

#### 10.2 迁移优势

| 功能      | @Scheduled      | XXL-JOB         |
| --------- | --------------- | --------------- |
| 修改 Cron | ❌ 需重启       | ✅ Web 界面修改 |
| 查看日志  | ❌ 查服务器日志 | ✅ Web 界面查看 |
| 手动触发  | ❌ 不支持       | ✅ 一键触发     |
| 分布式    | ❌ 需自己实现   | ✅ 原生支持     |
| 任务监控  | ❌ 无监控       | ✅ 完整监控     |

---

### 十一、下一步计划

#### 11.1 视频转码任务实现（优先级：高）

**任务**：

1. 安装 FFmpeg 工具
2. 创建视频转码工具类
3. 实现 videoTranscodeJob 逻辑
4. 配置定时任务（每 5 分钟）

**技术方案**：

```java
@XxlJob("videoTranscodeJob")
public void videoTranscodeJob() {
    // 1. 获取分片参数
    int shardIndex = XxlJobHelper.getShardIndex();
    int shardTotal = XxlJobHelper.getShardTotal();

    // 2. 查询待转码视频（按分片）
    List<MediaFiles> videos = mediaFileService.getPendingVideos(shardIndex, shardTotal);

    // 3. 批量转码
    for (MediaFiles video : videos) {
        try {
            // 调用FFmpeg转码
            String result = videoTranscoder.transcode(video);

            // 更新状态
            mediaFileService.updateTranscodeStatus(video.getId(), "success");

        } catch (Exception e) {
            mediaFileService.updateTranscodeStatus(video.getId(), "failed");
            log.error("视频转码失败", e);
        }
    }
}
```

#### 11.2 迁移现有定时任务（优先级：中）

**待迁移任务**：

- [x] 分块文件清理（已完成）
- [ ] 课程发布任务（后续）
- [ ] 订单超时关闭（后续）

#### 11.3 生产环境优化（优先级：低）

**调度中心集群**：

```
1. 部署2台调度中心（主备）
2. 配置MySQL主从
3. 配置Nginx负载均衡
```

**执行器多实例**：

```
1. 部署3台媒资服务
2. 配置分片广播策略
3. 测试故障转移
```

**监控告警**：

```
1. 配置邮件告警
2. 配置企业微信/钉钉告警
3. 对接Prometheus监控
```

---

### 十二、常见问题 FAQ

#### Q1：执行器无法注册？

**A**：检查步骤

1. Nacos 配置是否正确
2. XxlJobConfig 是否加载
3. 端口是否被占用
4. 调度中心是否启动

#### Q2：任务不执行？

**A**：检查步骤

1. Cron 表达式是否正确
2. JobHandler 名称是否匹配
3. 任务是否启动
4. 执行器是否在线

#### Q3：如何查看详细日志？

**A**：

- 调度中心日志：调度日志 → 执行日志
- 执行器日志：`D:/project/java/xuecheng-plus-project/logs/xxl-job/`
- 应用日志：`D:/project/java/xuecheng-plus-project/logs/`

#### Q4：如何手动触发任务？

**A**：

1. 调度中心 → 任务管理
2. 找到任务，点击"执行一次"
3. 查看调度日志

#### Q5：如何暂停任务？

**A**：

1. 调度中心 → 任务管理
2. 找到任务，点击"暂停"
3. 需要时再点击"启动"

---

### 十三、文件清单总结

#### 13.1 核心代码

```
xuecheng-plus-media/xuecheng-plus-media-service/
├── pom.xml                              ✅ XXL-JOB依赖
├── src/main/java/com/xuecheng/media/
│   ├── config/
│   │   └── XxlJobConfig.java           ✅ 执行器配置
│   ├── job/
│   │   └── MediaProcessJob.java        ✅ 任务处理器
│   └── task/
│       └── ChunkCleanupTask.java       ✅ 现有清理任务
```

#### 13.2 配置文件

```
项目根目录/
├── xxl-job-init.sql                     ✅ 数据库初始化
├── nacos-config-xxljob.yaml             ✅ Nacos配置示例
```

#### 13.3 文档文件

```
项目根目录/
├── XXL-JOB集成指南.md                   ✅ 详细文档
├── XXL-JOB快速启动指南.md               ✅ 快速上手
└── XXL-JOB集成完成总结.md              ✅ 本文档
```

---

## 🎉 总结

### 已完成 ✅

1. **理论讲解**：任务调度、分布式任务调度、XXL-JOB 架构
2. **环境搭建**：数据库创建、调度中心部署
3. **执行器集成**：依赖添加、配置类创建、任务处理器编写
4. **功能验证**：测试任务、分块清理任务
5. **文档编写**：完整的集成指南和快速启动指南

### 待实现 ⏳

1. **视频转码任务**：集成 FFmpeg，实现自动转码
2. **其他定时任务**：课程发布、订单关闭等
3. **生产优化**：集群部署、监控告警

### 核心收获 🎯

- ✅ 掌握分布式任务调度原理
- ✅ 掌握 XXL-JOB 的使用
- ✅ 具备实际项目集成能力
- ✅ 为后续功能打好基础

---

**下一步：开始实现视频转码功能！** 🚀
