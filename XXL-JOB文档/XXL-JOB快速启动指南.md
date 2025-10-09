# XXL-JOB 快速启动指南

## 🚀 5 分钟快速上手

本指南帮助你快速搭建并运行 XXL-JOB 分布式任务调度系统。

---

## 一、环境准备

### 1.1 必备软件

- ✅ JDK 8+
- ✅ MySQL 5.7+
- ✅ Maven 3.6+
- ✅ IDEA（推荐）

### 1.2 检查环境

```bash
# 检查Java版本
java -version

# 检查Maven版本
mvn -version

# 检查MySQL服务
mysql --version
```

---

## 二、调度中心搭建（3 步完成）

### 步骤 1：创建数据库

**方式 1：命令行执行**

```bash
# 进入项目目录
cd D:/project/java/xuecheng-plus-project

# 执行SQL脚本
mysql -uroot -pmysql < xxl-job-init.sql
```

**方式 2：可视化工具**

```
1. 打开Navicat/DBeaver
2. 新建查询
3. 打开并执行 xxl-job-init.sql 文件
```

**验证**：

```sql
USE xxl_job;
SHOW TABLES;
-- 应该显示8张表
```

### 步骤 2：下载并配置调度中心

**下载 XXL-JOB**：

- 下载地址：https://github.com/xuxueli/xxl-job/releases/tag/2.3.1
- 或使用已提供的：`xxl-job-2.3.1.zip`

**解压并打开**：

```bash
# 解压到工具目录
unzip xxl-job-2.3.1.zip -d D:/tools/

# 用IDEA打开
File → Open → 选择 D:/tools/xxl-job-2.3.1
```

**修改配置**：

```
文件：xxl-job-admin/src/main/resources/application.properties

修改内容：
spring.datasource.url=jdbc:mysql://127.0.0.1:3306/xxl_job?useUnicode=true&characterEncoding=UTF-8&autoReconnect=true&serverTimezone=Asia/Shanghai
spring.datasource.username=root
spring.datasource.password=mysql  # 改成你的MySQL密码
server.port=8888
```

### 步骤 3：启动调度中心

**IDEA 启动**：

```
1. 找到 xxl-job-admin 模块
2. 运行 XxlJobAdminApplication 主类
3. 等待启动完成（看到 "Started XxlJobAdminApplication" 日志）
```

**访问界面**：

```
URL: http://localhost:8888/xxl-job-admin
用户名: admin
密码: 123456
```

**🎉 调度中心搭建完成！**

---

## 三、执行器集成（4 步完成）

### 步骤 1：添加依赖（已完成 ✅）

文件：`xuecheng-plus-media/xuecheng-plus-media-service/pom.xml`

```xml
<!-- 已添加 -->
<dependency>
    <groupId>com.xuxueli</groupId>
    <artifactId>xxl-job-core</artifactId>
    <version>2.3.1</version>
</dependency>
```

### 步骤 2：添加 Nacos 配置

**操作**：

```
1. 登录Nacos：http://localhost:8848/nacos
2. 找到配置：media-service-dev.yaml
3. 添加以下内容到文件末尾：
```

```yaml
# XXL-JOB配置
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

```
4. 点击"发布"保存配置
```

### 步骤 3：在调度中心注册执行器

**操作**：

```
1. 访问：http://localhost:8888/xxl-job-admin
2. 点击：执行器管理 → 新增
3. 填写：
   - AppName: media-service-executor
   - 名称: 媒资服务执行器
   - 注册方式: 自动注册
4. 点击：保存
```

### 步骤 4：启动媒资服务

**IDEA 启动**：

```
1. 找到 MediaApplication 主类
2. 右键 → Run 'MediaApplication'
3. 查看启动日志
```

**验证启动成功**：

```
日志关键字：
✅ "XXL-JOB 执行器配置完成"
✅ "调度中心地址: http://127.0.0.1:8888/xxl-job-admin"
✅ "执行器名称: media-service-executor"
✅ "执行器端口: 9999"
```

**验证注册成功**：

```
1. 刷新调度中心页面
2. 点击：执行器管理
3. 找到 "媒资服务执行器"
4. 点击 "在线机器" → 应该显示 1（绿色）
5. 点击数字查看详情 → 应该显示本机IP和端口
```

**🎉 执行器集成完成！**

---

## 四、创建第一个任务（3 步完成）

### 步骤 1：创建任务

**操作**：

```
1. 登录调度中心
2. 点击：任务管理 → 新增
3. 填写基础配置：
```

| 配置项     | 值                  | 说明                           |
| ---------- | ------------------- | ------------------------------ |
| 执行器     | 媒资服务执行器      | 下拉选择                       |
| 任务描述   | 测试任务            | 任务名称                       |
| 路由策略   | 第一个              | 单机执行                       |
| Cron       | `0/10 * * * * ?`    | 每 10 秒执行                   |
| 运行模式   | BEAN                | 选择 BEAN 模式                 |
| JobHandler | `testJob`           | 对应代码中的@XxlJob("testJob") |
| 任务参数   | `{"test": "hello"}` | 可选                           |

```
4. 高级配置（保持默认）：
   - 阻塞处理策略: 单机串行
   - 任务超时时间: 0
   - 失败重试次数: 0

5. 点击：保存
```

### 步骤 2：启动任务

**操作**：

```
1. 在任务列表找到刚创建的任务
2. 点击：启动 按钮（绿色）
3. 确认任务状态变为 "运行中"
```

### 步骤 3：查看执行日志

**操作**：

```
1. 点击：调度日志（顶部菜单）
2. 找到任务执行记录
3. 点击：执行日志 按钮
4. 查看详细输出
```

**预期日志**：

```
========== 测试任务开始执行 ==========
执行时间：2025-10-07 22:00:00
任务参数：{"test": "hello"}
任务执行中...
========== 测试任务执行完成 ==========
```

**🎉 第一个任务创建成功！**

---

## 五、常见问题排查

### 问题 1：执行器无法注册

**现象**：调度中心 → 执行器管理 → 在线机器显示 0

**排查步骤**：

1. **检查配置文件**

   ```bash
   # 登录Nacos查看配置
   http://localhost:8848/nacos
   # 确认 media-service-dev.yaml 中有 xxl.job 配置
   ```

2. **检查服务启动日志**

   ```bash
   # 搜索关键字
   XXL-JOB 执行器配置
   # 应该看到配置信息
   ```

3. **检查端口占用**

   ```bash
   netstat -ano | findstr "9999"
   # 应该有 LISTENING 状态
   ```

4. **检查配置类**
   ```bash
   # 确认存在文件
   XxlJobConfig.java
   # 确认有 @Configuration 注解
   ```

### 问题 2：任务不执行

**现象**：任务已启动，但调度日志无记录

**排查步骤**：

1. **检查 Cron 表达式**

   ```
   在线验证：https://cron.qqe2.com/
   输入：0/10 * * * * ?
   查看下次执行时间
   ```

2. **检查 JobHandler 名称**

   ```java
   // 代码中
   @XxlJob("testJob")

   // 调度中心任务配置
   JobHandler: testJob

   // 确保一致
   ```

3. **检查任务状态**
   ```
   调度中心 → 任务管理
   确认状态为 "运行中"（绿色）
   ```

### 问题 3：任务报错

**现象**：调度日志显示失败

**排查步骤**：

1. **查看调度日志**

   ```
   调度中心 → 调度日志
   点击 "执行日志" 查看错误信息
   ```

2. **查看执行器日志**

   ```
   路径：D:/project/java/xuecheng-plus-project/logs/xxl-job/
   文件：xxl-job-executor.log
   ```

3. **查看应用日志**
   ```
   路径：D:/project/java/xuecheng-plus-project/logs/
   文件：media-api-error.log
   ```

---

## 六、Cron 表达式速查

```
格式：秒 分 时 日 月 周

常用示例：
0/10 * * * * ?      → 每10秒执行
0 0/5 * * * ?       → 每5分钟执行
0 0 2 * * ?         → 每天凌晨2点执行
0 0 8 ? * MON       → 每周一早上8点执行
0 0 0 1 * ?         → 每月1号凌晨执行
0 0 12 * * ?        → 每天中午12点执行
0 15 10 ? * MON-FRI → 工作日上午10:15执行

在线工具：
https://cron.qqe2.com/
```

---

## 七、下一步计划

### 7.1 整合现有任务

将 Spring `@Scheduled`任务迁移到 XXL-JOB：

**文件**：`MediaProcessJob.java`

```java
// 分块文件清理任务
@XxlJob("chunkCleanupJob")
public void chunkCleanupJob() {
    chunkCleanupTask.cleanupOldChunks();
}
```

**配置任务**：

- Cron: `0 0 2 * * ?` （每天凌晨 2 点）
- JobHandler: `chunkCleanupJob`

### 7.2 实现视频转码任务

1. 安装 FFmpeg 工具
2. 创建转码工具类
3. 实现转码任务逻辑
4. 配置定时任务（每 5 分钟）

### 7.3 生产环境部署

1. 调度中心集群部署（2 台）
2. 执行器多实例部署（3 台）
3. 配置任务分片策略
4. 配置监控告警

---

## 八、核心文件清单

### 已创建的文件

```
项目根目录/
├── XXL-JOB集成指南.md              # 详细集成文档
├── XXL-JOB快速启动指南.md          # 本文档
├── xxl-job-init.sql                 # 数据库初始化脚本
├── nacos-config-xxljob.yaml         # Nacos配置示例
│
└── xuecheng-plus-media/
    └── xuecheng-plus-media-service/
        ├── pom.xml                   # ✅ 已添加XXL-JOB依赖
        └── src/main/java/com/xuecheng/media/
            ├── config/
            │   └── XxlJobConfig.java # ✅ 执行器配置类
            └── job/
                └── MediaProcessJob.java # ✅ 任务处理器
```

---

## 🎉 恭喜！你已成功集成 XXL-JOB！

现在你可以：

- ✅ 在 Web 界面管理任务
- ✅ 动态修改 Cron 表达式
- ✅ 查看任务执行日志
- ✅ 支持分布式执行
- ✅ 故障自动转移

---

## 📚 参考资料

- **官方文档**：https://www.xuxueli.com/xxl-job/
- **GitHub**：https://github.com/xuxueli/xxl-job
- **Cron 在线工具**：https://cron.qqe2.com/
- **视频教程**：https://www.bilibili.com/video/BV1j8411N7Bm/

---

**有问题？查看 `XXL-JOB集成指南.md` 获取详细说明！**
