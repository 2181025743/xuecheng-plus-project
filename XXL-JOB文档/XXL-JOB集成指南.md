# XXL-JOB 分布式任务调度集成指南

## 📋 目录

1. [XXL-JOB 简介](#一xxl-job-简介)
2. [调度中心搭建](#二调度中心搭建)
3. [执行器集成](#三执行器集成)
4. [任务配置](#四任务配置)
5. [常见问题](#五常见问题)

---

## 一、XXL-JOB 简介

### 1.1 什么是 XXL-JOB？

**XXL-JOB** 是一个轻量级分布式任务调度平台，核心设计目标是：

- **简单**：提供 Web 界面，轻松管理调度任务
- **动态**：支持动态修改任务状态、启动/停止任务，以及终止运行中任务
- **分布式**：支持集群部署，提高系统可用性和任务处理能力
- **弹性扩容**：动态添加执行器节点

### 1.2 核心架构

```
                 ┌─────────────────────────┐
                 │   调度中心 (Admin)       │
                 │  - 任务管理              │
                 │  - 调度触发              │
                 │  - 日志查询              │
                 │  Port: 8080             │
                 └─────────────────────────┘
                           ↓ ↑
        ┌──────────────────┴─┴──────────────────┐
        ↓                                        ↓
┌─────────────────┐                    ┌─────────────────┐
│  执行器1         │                    │  执行器2         │
│  (媒资服务)      │                    │  (订单服务)      │
│  Port: 9999     │                    │  Port: 9998     │
└─────────────────┘                    └─────────────────┘
```

### 1.3 工作流程

1. **执行器注册**：执行器启动时，自动注册到调度中心
2. **任务调度**：调度中心根据 Cron 表达式，定时触发任务
3. **任务执行**：执行器接收任务，通过线程池执行
4. **结果回调**：执行器将执行结果异步上报给调度中心

---

## 二、调度中心搭建

### 2.1 下载 XXL-JOB

**方式 1：GitHub 下载**

```bash
# 访问 GitHub 下载 2.3.1 版本
https://github.com/xuxueli/xxl-job/releases/tag/2.3.1
```

**方式 2：码云下载（国内推荐）**

```bash
# 访问码云下载
https://gitee.com/xuxueli0323/xxl-job/releases/tag/2.3.1
```

**方式 3：直接下载（已提供）**

- 文件：`xxl-job-2.3.1.zip`
- 解压到：`D:/tools/xxl-job-2.3.1/`

### 2.2 创建数据库

**步骤 1：创建数据库**

```sql
CREATE DATABASE IF NOT EXISTS `xxl_job` DEFAULT CHARACTER SET utf8mb4;
```

**步骤 2：执行初始化脚本**

- 脚本位置：`xxl-job-2.3.1/doc/db/tables_xxl_job.sql`
- 执行方式：

  ```bash
  # 命令行执行
  mysql -uroot -p xxl_job < D:/tools/xxl-job-2.3.1/doc/db/tables_xxl_job.sql

  # 或者在Navicat/DBeaver中直接运行SQL脚本
  ```

**脚本内容概览**：

```sql
-- 8张核心表
xxl_job_info           -- 任务信息表
xxl_job_log            -- 任务日志表
xxl_job_log_report     -- 日志报表
xxl_job_logglue        -- GLUE脚本
xxl_job_registry       -- 执行器注册表
xxl_job_group          -- 执行器分组
xxl_job_user           -- 用户表
xxl_job_lock           -- 分布式锁
```

### 2.3 配置调度中心

**步骤 1：修改配置文件**

- 文件位置：`xxl-job-admin/src/main/resources/application.properties`

```properties
### 数据库配置
spring.datasource.url=jdbc:mysql://127.0.0.1:3306/xxl_job?useUnicode=true&characterEncoding=UTF-8&autoReconnect=true&serverTimezone=Asia/Shanghai
spring.datasource.username=root
spring.datasource.password=mysql
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

### 调度中心端口
server.port=8888

### 日志配置
xxl.job.accessToken=
xxl.job.i18n=zh_CN

### 调度线程池配置
xxl.job.triggerpool.fast.max=200
xxl.job.triggerpool.slow.max=100

### 日志保留天数
xxl.job.logretentiondays=30
```

**步骤 2：启动调度中心**

**方式 1：IDEA 启动（推荐开发环境）**

```
1. 用IDEA打开 xxl-job-admin 项目
2. 修改application.properties中的数据库配置
3. 运行 XxlJobAdminApplication 主类
4. 访问：http://localhost:8888/xxl-job-admin
```

**方式 2：打包启动（推荐生产环境）**

```bash
# 1. 打包
cd xxl-job-admin
mvn clean package -DskipTests

# 2. 启动
java -jar target/xxl-job-admin-2.3.1.jar

# 3. 后台启动
nohup java -jar target/xxl-job-admin-2.3.1.jar > xxl-job-admin.log 2>&1 &
```

### 2.4 登录调度中心

**访问地址**：`http://localhost:8888/xxl-job-admin`

**默认账号**：

- 用户名：`admin`
- 密码：`123456`

**界面功能**：

- **执行器管理**：查看/管理执行器
- **任务管理**：创建/编辑/删除任务
- **调度日志**：查看任务执行日志
- **用户管理**：管理调度中心用户

---

## 三、执行器集成

### 3.1 添加依赖（媒资服务）

**文件**：`xuecheng-plus-media/xuecheng-plus-media-service/pom.xml`

```xml
<!-- XXL-JOB 核心依赖 -->
<dependency>
    <groupId>com.xuxueli</groupId>
    <artifactId>xxl-job-core</artifactId>
    <version>2.3.1</version>
</dependency>
```

### 3.2 配置执行器（Nacos 配置）

**配置文件**：`media-service-dev.yaml`（Nacos 中）

```yaml
xxl:
  job:
    admin:
      addresses: http://127.0.0.1:8888/xxl-job-admin # 调度中心地址
    executor:
      appname: media-service-executor # 执行器名称（需要在调度中心注册）
      port: 9999 # 执行器端口
      logpath: D:/project/java/xuecheng-plus-project/logs/xxl-job # 日志路径
      logretentiondays: 30 # 日志保留天数
```

**配置说明**：

- `addresses`：调度中心的完整 URL
- `appname`：执行器的唯一标识，**必须先在调度中心注册**
- `port`：执行器通讯端口，调度中心通过此端口下发任务
- `logpath`：执行器任务日志存储路径
- `logretentiondays`：日志保留天数，超过自动清理

### 3.3 创建配置类

**文件**：`xuecheng-plus-media/xuecheng-plus-media-service/src/main/java/com/xuecheng/media/config/XxlJobConfig.java`

```java
package com.xuecheng.media.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * XXL-JOB 执行器配置
 */
@Slf4j
@Configuration
public class XxlJobConfig {

    @Value("${xxl.job.admin.addresses}")
    private String adminAddresses;

    @Value("${xxl.job.executor.appname}")
    private String appname;

    @Value("${xxl.job.executor.port}")
    private int port;

    @Value("${xxl.job.executor.logpath}")
    private String logPath;

    @Value("${xxl.job.executor.logretentiondays}")
    private int logRetentionDays;

    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {
        log.info(">>>>>>>>>>> XXL-JOB 执行器配置初始化.");

        XxlJobSpringExecutor xxlJobSpringExecutor = new XxlJobSpringExecutor();
        xxlJobSpringExecutor.setAdminAddresses(adminAddresses);
        xxlJobSpringExecutor.setAppname(appname);
        xxlJobSpringExecutor.setPort(port);
        xxlJobSpringExecutor.setLogPath(logPath);
        xxlJobSpringExecutor.setLogRetentionDays(logRetentionDays);

        log.info(">>>>>>>>>>> XXL-JOB 执行器配置完成：");
        log.info("  - 调度中心地址: {}", adminAddresses);
        log.info("  - 执行器名称: {}", appname);
        log.info("  - 执行器端口: {}", port);
        log.info("  - 日志路径: {}", logPath);

        return xxlJobSpringExecutor;
    }
}
```

### 3.4 在调度中心注册执行器

**步骤**：

1. 登录调度中心：`http://localhost:8888/xxl-job-admin`
2. 点击：**执行器管理** → **新增**
3. 填写信息：
   - **AppName**：`media-service-executor`（与配置文件一致）
   - **名称**：`媒资服务执行器`
   - **注册方式**：自动注册
   - **机器地址**：自动获取（执行器启动后自动填充）
4. 点击：**保存**

---

## 四、任务配置

### 4.1 编写任务处理器

**文件**：`xuecheng-plus-media/xuecheng-plus-media-service/src/main/java/com/xuecheng/media/job/MediaProcessJob.java`

```java
package com.xuecheng.media.job;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 媒资处理任务
 */
@Slf4j
@Component
public class MediaProcessJob {

    /**
     * 测试任务
     */
    @XxlJob("testJob")
    public void testJob() {
        log.info("========== 测试任务开始执行 ==========");
        log.info("当前时间：{}", new java.util.Date());
        log.info("任务参数：{}", XxlJobHelper.getJobParam());
        log.info("========== 测试任务执行完成 ==========");
    }

    /**
     * 视频转码任务（后续实现）
     */
    @XxlJob("videoTranscodeJob")
    public void videoTranscodeJob() {
        log.info("========== 视频转码任务开始 ==========");

        // 获取分片参数（支持分布式执行）
        int shardIndex = XxlJobHelper.getShardIndex();  // 当前分片索引
        int shardTotal = XxlJobHelper.getShardTotal();  // 总分片数

        log.info("分片参数：当前分片={}/{}", shardIndex, shardTotal);

        // TODO: 查询待转码视频
        // TODO: 执行转码逻辑

        log.info("========== 视频转码任务完成 ==========");
    }

    /**
     * 分块文件清理任务（整合现有功能）
     */
    @XxlJob("chunkCleanupJob")
    public void chunkCleanupJob() {
        log.info("========== 分块文件清理任务开始 ==========");

        // TODO: 调用现有的 ChunkCleanupTask.cleanupOldChunks()

        log.info("========== 分块文件清理任务完成 ==========");
    }
}
```

**关键注解说明**：

- `@XxlJob("任务名")`：定义任务处理器，名称需与调度中心配置一致
- `XxlJobHelper.getJobParam()`：获取任务参数
- `XxlJobHelper.getShardIndex/Total()`：获取分片参数（用于分布式任务）

### 4.2 在调度中心创建任务

**步骤**：

1. 登录调度中心
2. 点击：**任务管理** → **新增**
3. 填写任务信息：

**基础配置**：

- **执行器**：选择 `媒资服务执行器`
- **任务描述**：`测试任务`
- **路由策略**：`第一个`（单机）或 `分片广播`（分布式）
- **Cron 表达式**：`0/10 * * * * ?`（每 10 秒执行一次）
- **运行模式**：`BEAN`
- **JobHandler**：`testJob`（与@XxlJob 注解的值一致）
- **任务参数**：（可选）`{"test": "hello"}`

**高级配置**：

- **阻塞处理策略**：单机串行（推荐）
- **任务超时时间**：0（不限制）
- **失败重试次数**：0（不重试）

4. 点击：**保存**
5. 点击：**启动** 按钮

### 4.3 查看任务执行日志

**步骤**：

1. 点击：**调度日志**
2. 选择任务，点击：**执行日志**
3. 查看详细日志输出

---

## 五、常见问题

### 5.1 执行器无法注册

**现象**：调度中心 → 执行器管理 → 在线机器列表为空

**排查步骤**：

1. **检查配置文件**

   ```yaml
   # 确认 appname 与调度中心注册的一致
   xxl.job.executor.appname: media-service-executor
   ```

2. **检查依赖**

   ```xml
   <!-- 确认已添加依赖 -->
   <dependency>
       <groupId>com.xuxueli</groupId>
       <artifactId>xxl-job-core</artifactId>
   </dependency>
   ```

3. **检查配置类**

   ```java
   // 确认已创建 XxlJobConfig 配置类
   @Bean
   public XxlJobSpringExecutor xxlJobExecutor() { ... }
   ```

4. **检查启动日志**

   ```
   查找关键字：XXL-JOB 执行器配置
   应该显示：注册成功
   ```

5. **检查端口占用**
   ```bash
   netstat -ano | findstr "9999"
   ```

### 5.2 任务不执行

**现象**：任务已启动，但调度日志无记录

**排查步骤**：

1. **检查 Cron 表达式**
   - 使用在线工具验证：https://cron.qqe2.com/
2. **检查 JobHandler 名称**

   ```java
   // 确保一致
   @XxlJob("testJob")  // 代码中的名称

   // 调度中心任务配置的 JobHandler 也是 testJob
   ```

3. **检查任务状态**

   - 调度中心 → 任务管理 → 确认任务为"运行中"

4. **检查执行器在线状态**
   - 调度中心 → 执行器管理 → 确认有在线机器

### 5.3 任务报错

**现象**：调度日志显示失败

**排查步骤**：

1. **查看执行器日志**

   ```
   路径：D:/project/java/xuecheng-plus-project/logs/xxl-job/
   文件：xxl-job-executor.log
   ```

2. **查看调度中心日志**

   - 调度日志 → 执行日志 → 查看详细错误信息

3. **常见错误**
   - `Bean not found`：JobHandler 名称配置错误
   - `Timeout`：任务执行超时
   - `Null Pointer`：代码逻辑错误

### 5.4 分片任务不生效

**现象**：多个执行器实例，但只有一个在执行任务

**解决方案**：

1. **确认路由策略**

   - 调度中心 → 任务管理 → 编辑任务
   - 路由策略选择：`分片广播`

2. **确认执行器在线**

   - 执行器管理 → 查看在线机器数量

3. **代码中获取分片参数**

   ```java
   int shardIndex = XxlJobHelper.getShardIndex();
   int shardTotal = XxlJobHelper.getShardTotal();

   // 根据分片参数处理不同数据
   ```

---

## 六、路由策略说明

| 策略               | 说明                         | 适用场景           |
| ------------------ | ---------------------------- | ------------------ |
| **第一个**         | 固定选择第一个机器           | 单机任务           |
| **最后一个**       | 固定选择最后一个机器         | 单机任务           |
| **轮询**           | 依次选择机器                 | 负载均衡           |
| **随机**           | 随机选择机器                 | 负载均衡           |
| **一致性 HASH**    | 相同参数路由到同一机器       | 需要状态保持的任务 |
| **最不经常使用**   | 选择使用频率最低的机器       | 负载均衡           |
| **最近最久未使用** | 选择最久未使用的机器         | 负载均衡           |
| **故障转移**       | 按顺序测试，第一个可用的执行 | 高可用             |
| **忙碌转移**       | 第一个忙则转移到下一个       | 高可用             |
| **分片广播**       | 广播所有机器，每个执行一部分 | **分布式任务** ⭐  |

**推荐配置**：

- **单机任务**：第一个
- **负载均衡**：轮询
- **分布式大任务**：分片广播

---

## 七、下一步计划

### 7.1 视频转码任务集成

1. 安装 FFmpeg 工具
2. 创建视频转码工具类
3. 编写转码任务逻辑
4. 配置定时任务（每 5 分钟检查一次）

### 7.2 整合现有定时任务

将现有的 Spring `@Scheduled`任务迁移到 XXL-JOB：

- ✅ 分块文件清理任务
- ⏳ 课程发布任务（后续）
- ⏳ 订单超时关闭（后续）

### 7.3 生产环境优化

1. 调度中心集群部署
2. 执行器多实例部署
3. 任务监控告警配置
4. 日志持久化策略

---

## 八、参考资料

- **官方文档**：https://www.xuxueli.com/xxl-job/
- **GitHub**：https://github.com/xuxueli/xxl-job
- **码云**：https://gitee.com/xuxueli0323/xxl-job
- **Cron 表达式在线工具**：https://cron.qqe2.com/

---

**集成完成后，你的项目将具备强大的分布式任务调度能力！** 🎉
