# 🚀 开始使用 XXL-JOB

## 📋 快速导航

你现在有以下文档可以查阅：

### 1. 快速上手（推荐先看这个）

📄 **文件**：`XXL-JOB快速启动指南.md`  
⏱️ **时间**：5-10 分钟  
📝 **内容**：

- 环境准备
- 调度中心搭建（3 步）
- 执行器集成（4 步）
- 创建第一个任务（3 步）
- 常见问题排查

**适合**：想快速体验 XXL-JOB 功能

---

### 2. 详细文档（遇到问题时查看）

📄 **文件**：`XXL-JOB集成指南.md`  
⏱️ **时间**：20-30 分钟  
📝 **内容**：

- XXL-JOB 架构原理
- 完整集成步骤
- 配置详解
- 路由策略说明
- 常见问题 FAQ
- 下一步计划

**适合**：深入了解 XXL-JOB，排查复杂问题

---

### 3. 工作总结（了解已完成的工作）

📄 **文件**：`XXL-JOB集成完成总结.md`  
⏱️ **时间**：10-15 分钟  
📝 **内容**：

- 核心概念讲解
- 已完成的工作清单
- 功能验证清单
- 分片任务详解
- 对比@Scheduled
- 下一步计划

**适合**：回顾整个集成过程，规划后续工作

---

## 🎯 立即开始（3 步走）

### 第 1 步：搭建调度中心

#### 选项 A：使用已有脚本（推荐）

```bash
# 1. 创建数据库
mysql -uroot -pmysql < xxl-job-init.sql

# 2. 下载XXL-JOB（2.3.1版本）
# 从 https://github.com/xuxueli/xxl-job/releases/tag/2.3.1 下载
# 或使用已提供的 xxl-job-2.3.1.zip

# 3. 解压到工具目录
unzip xxl-job-2.3.1.zip -d D:/tools/

# 4. 用IDEA打开并修改配置
# 文件：xxl-job-admin/src/main/resources/application.properties
# 修改数据库连接、端口

# 5. 启动调度中心
# 运行 XxlJobAdminApplication 主类
```

#### 选项 B：使用 Docker（更简单）

```bash
# 1. 启动MySQL（如果未启动）
docker run -d --name mysql \
  -e MYSQL_ROOT_PASSWORD=mysql \
  -p 3306:3306 mysql:5.7

# 2. 创建数据库
mysql -uroot -pmysql < xxl-job-init.sql

# 3. 启动XXL-JOB调度中心
docker run -d --name xxl-job-admin \
  -p 8888:8080 \
  -e PARAMS="--spring.datasource.url=jdbc:mysql://host.docker.internal:3306/xxl_job?useUnicode=true&characterEncoding=UTF-8&autoReconnect=true&serverTimezone=Asia/Shanghai --spring.datasource.username=root --spring.datasource.password=mysql" \
  xuxueli/xxl-job-admin:2.3.1

# 4. 访问界面
http://localhost:8888/xxl-job-admin
账号：admin / 123456
```

---

### 第 2 步：配置执行器

#### 1. 添加 Nacos 配置

```yaml
# 登录Nacos：http://localhost:8848/nacos
# 编辑配置：media-service-dev.yaml
# 添加以下内容：

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

#### 2. 在调度中心注册执行器

```
1. 访问：http://localhost:8888/xxl-job-admin
2. 点击：执行器管理 → 新增
3. 填写：
   AppName: media-service-executor
   名称: 媒资服务执行器
   注册方式: 自动注册
4. 保存
```

#### 3. 启动媒资服务

```
IDEA中启动 MediaApplication
查看日志确认执行器注册成功
```

---

### 第 3 步：创建并运行任务

#### 1. 创建任务

```
1. 调度中心 → 任务管理 → 新增
2. 填写：
   执行器: 媒资服务执行器
   任务描述: 测试任务
   Cron: 0/10 * * * * ?
   运行模式: BEAN
   JobHandler: testJob
3. 保存
```

#### 2. 启动任务

```
任务列表 → 找到任务 → 点击"启动"
```

#### 3. 查看日志

```
调度日志 → 找到执行记录 → 点击"执行日志"
```

---

## 📚 核心文件说明

### 代码文件

```
xuecheng-plus-media/xuecheng-plus-media-service/
├── pom.xml                              # ✅ 已添加XXL-JOB依赖
├── src/main/java/com/xuecheng/media/
│   ├── config/
│   │   └── XxlJobConfig.java           # ✅ 执行器配置类
│   └── job/
│       └── MediaProcessJob.java        # ✅ 任务处理器（包含4个示例任务）
```

### 配置文件

```
项目根目录/
├── xxl-job-init.sql                     # 数据库初始化脚本
├── nacos-config-xxljob.yaml             # Nacos配置示例
```

### 文档文件

```
项目根目录/
├── XXL-JOB快速启动指南.md               # ⭐ 5分钟快速上手
├── XXL-JOB集成指南.md                   # 📖 详细文档
├── XXL-JOB集成完成总结.md              # 📝 工作总结
└── 开始使用XXL-JOB.md                  # 📋 本文档（导航）
```

---

## 🎯 已实现的任务

| 任务名   | JobHandler        | 功能                     | 状态          |
| -------- | ----------------- | ------------------------ | ------------- |
| 测试任务 | testJob           | 测试 XXL-JOB 功能        | ✅ 可用       |
| 分块清理 | chunkCleanupJob   | 清理 24 小时前的分块文件 | ✅ 可用       |
| 视频转码 | videoTranscodeJob | 批量视频转码             | ⏳ 框架已搭建 |
| 参数示例 | paramJob          | 演示参数使用             | ✅ 可用       |

---

## 🔥 推荐的学习路径

### 第 1 天：快速体验

1. ⏱️ 5 分钟：阅读 `XXL-JOB快速启动指南.md`
2. ⏱️ 10 分钟：搭建调度中心
3. ⏱️ 5 分钟：配置执行器
4. ⏱️ 5 分钟：创建并运行测试任务
5. ⏱️ 5 分钟：查看执行日志

**目标**：能够成功运行第一个任务 ✅

---

### 第 2 天：深入理解

1. ⏱️ 20 分钟：阅读 `XXL-JOB集成指南.md`
2. ⏱️ 10 分钟：了解路由策略
3. ⏱️ 10 分钟：了解分片任务
4. ⏱️ 10 分钟：配置分块清理任务
5. ⏱️ 10 分钟：查看任务执行效果

**目标**：理解 XXL-JOB 核心概念 ✅

---

### 第 3 天：实战应用

1. ⏱️ 30 分钟：实现视频转码任务
2. ⏱️ 20 分钟：配置分片广播策略
3. ⏱️ 10 分钟：测试多实例分片

**目标**：能够独立开发任务 ✅

---

## ⚠️ 注意事项

### 1. 端口占用

确保以下端口未被占用：

- `3306` - MySQL
- `8848` - Nacos
- `8888` - XXL-JOB 调度中心
- `9999` - XXL-JOB 执行器
- `63050` - 媒资服务

```bash
# 检查端口
netstat -ano | findstr "8888"
netstat -ano | findstr "9999"
```

### 2. 配置一致性

**关键配置必须一致**：

- Nacos 中的 `xxl.job.executor.appname`
- 调度中心注册的执行器 `AppName`
- 必须完全一致，否则无法注册

### 3. 日志目录

确保日志目录存在且有写权限：

```
D:/project/java/xuecheng-plus-project/logs/xxl-job/
```

如果目录不存在，会自动创建。

### 4. Cron 表达式

使用在线工具验证：https://cron.qqe2.com/

常见错误：

- `* * * * * ?` - 每秒执行（过于频繁，不推荐）
- `0 0 0 * * ?` - 每天 0 点（正确）
- `0 0 2 * * ?` - 每天 2 点（正确）

---

## 🐛 常见问题速查

### 问题 1：执行器无法注册

**解决**：

1. 检查 Nacos 配置是否正确
2. 检查 XxlJobConfig 是否加载
3. 检查端口 9999 是否被占用
4. 检查调度中心是否启动

### 问题 2：任务不执行

**解决**：

1. 检查 Cron 表达式
2. 检查 JobHandler 名称
3. 检查任务是否启动
4. 检查执行器是否在线

### 问题 3：任务报错

**解决**：

1. 查看调度日志
2. 查看执行器日志
3. 查看应用日志

**详细排查步骤请查看 `XXL-JOB快速启动指南.md` 第五章**

---

## 📞 获取帮助

### 文档资源

- ✅ `XXL-JOB快速启动指南.md` - 快速上手
- ✅ `XXL-JOB集成指南.md` - 详细文档
- ✅ `XXL-JOB集成完成总结.md` - 工作总结

### 在线资源

- 官方文档：https://www.xuxueli.com/xxl-job/
- GitHub：https://github.com/xuxueli/xxl-job
- Cron 工具：https://cron.qqe2.com/

### 视频教程

- 黑马程序员：https://www.bilibili.com/video/BV1j8411N7Bm/

---

## 🎉 下一步计划

### 立即可做

- [x] 搭建调度中心
- [x] 集成执行器
- [x] 运行测试任务
- [x] 运行分块清理任务

### 后续计划

- [ ] 实现视频转码任务
- [ ] 配置分片广播
- [ ] 迁移其他定时任务
- [ ] 生产环境部署

---

**🚀 立即开始：打开 `XXL-JOB快速启动指南.md` 开始你的 XXL-JOB 之旅！**
