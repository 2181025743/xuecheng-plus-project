# 学成在线 - 在线教育平台

> 基于 SpringCloud、SpringCloudAlibaba 技术栈的企业级微服务项目

![](https://img.shields.io/badge/SpringBoot-2.7.9-green.svg)
![](https://img.shields.io/badge/SpringCloud-2021.0.5-blue.svg)
![](https://img.shields.io/badge/Nacos-2.1.0-brightgreen.svg)
![](https://img.shields.io/badge/XXL--JOB-2.3.1-orange.svg)
![](https://img.shields.io/badge/Vue-3.0-brightgreen.svg)
![](https://img.shields.io/badge/MySQL-8.0-blue.svg)

---

## 📚 项目介绍

学成在线是一个完整的在线教育平台，包含课程管理、视频处理、用户系统、支付学习等完整功能。

### 核心功能

- 📖 **内容管理**：课程发布、课程分类、课程计划
- 🎥 **媒资管理**：视频上传、断点续传、视频转码
- 👥 **用户系统**：用户注册、登录、权限管理
- 💰 **订单支付**：课程选购、在线支付
- 📊 **系统管理**：数据字典、系统配置

---

## 🏗️ 技术架构

### 后端技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| SpringBoot | 2.7.9 | 基础框架 |
| SpringCloud | 2021.0.5 | 微服务框架 |
| SpringCloud Alibaba | 2021.0.5.0 | 微服务组件 |
| Nacos | 2.1.0 | 配置中心&注册中心 |
| Gateway | - | 网关服务 |
| XXL-JOB | 2.3.1 | 分布式任务调度 |
| MyBatis-Plus | 3.5.3 | ORM 框架 |
| MySQL | 8.0 | 数据库 |
| Redis | - | 缓存 |
| RabbitMQ | - | 消息队列 |
| Elasticsearch | - | 搜索引擎 |
| MinIO/OSS | - | 对象存储 |

### 前端技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.0 | 前端框架 |
| TypeScript | - | 类型系统 |
| Element Plus | - | UI 组件库 |
| Axios | - | HTTP 客户端 |

---

## 📂 项目结构

```
xuecheng-plus-project/
├── xuecheng-plus-base/              # 基础模块
├── xuecheng-plus-content/           # 内容管理服务
├── xuecheng-plus-media/             # 媒资管理服务
├── xuecheng-plus-system/            # 系统管理服务
├── xuecheng-plus-gateway/           # 网关服务
├── xuecheng-plus-parent/            # 父工程
├── xxl-job-2.3.1/                   # XXL-JOB 调度中心
├── project-xczx2-portal-vue-ts/     # 前端项目
├── nacos/                            # Nacos 配置文件
└── XXL-JOB文档/                     # XXL-JOB 学习文档 ⭐
```

---

## 🚀 快速开始

### 前置环境

- ✅ JDK 1.8+
- ✅ Maven 3.6+
- ✅ MySQL 8.0+
- ✅ Redis
- ✅ Nacos 2.1.0
- ✅ Node.js 14+

### 启动步骤

#### 1. 配置敏感信息

⚠️ **重要**：请先阅读 [配置说明-重要.md](./配置说明-重要.md)

需要配置：
- 阿里云 OSS AccessKey
- MySQL 数据库连接
- Redis 连接等

#### 2. 启动基础服务

```bash
# 启动 MySQL
# 启动 Redis
# 启动 Nacos（端口：8848）
```

#### 3. 启动 XXL-JOB Admin

```bash
cd xxl-job-2.3.1/xxl-job-admin
mvn spring-boot:run
```

访问：http://127.0.0.1:8080/xxl-job-admin  
用户名/密码：admin / 123456

#### 4. 启动后端服务

```bash
# 内容管理服务
cd xuecheng-plus-content/xuecheng-plus-content-api
mvn spring-boot:run

# 媒资管理服务
cd xuecheng-plus-media/xuecheng-plus-media-api
mvn spring-boot:run

# 系统管理服务
cd xuecheng-plus-system/xuecheng-plus-system-api
mvn spring-boot:run

# 网关服务
cd xuecheng-plus-gateway
mvn spring-boot:run
```

#### 5. 启动前端

```bash
cd project-xczx2-portal-vue-ts
npm install
npm run serve
```

访问：http://localhost:8601

---

## 📖 核心功能说明

### 1. 大文件断点续传

- ✅ 前端分块上传
- ✅ 后端合并验证
- ✅ MD5 秒传
- ✅ 断点续传

详见：[XXL-JOB文档/断点续传完整流程详解.md](./XXL-JOB文档/断点续传完整流程详解.md)

### 2. XXL-JOB 分布式任务调度 ⭐核心

**已实现的任务**：
- `testJob` - 测试任务
- `shardingJobHandler` - 分片广播测试任务
- `videoTranscodeJob` - 视频转码任务（框架）
- `chunkCleanupJob` - 分块文件清理任务
- `paramJob` - 参数任务示例

**学习文档**：
- 📖 [XXL-JOB文档/README.md](./XXL-JOB文档/README.md) - 文档导航
- 🚀 [快速开始-分片广播测试.md](./XXL-JOB文档/快速开始-分片广播测试.md) - 5分钟上手
- 📝 [视频笔记-分片广播机制详解.md](./XXL-JOB文档/视频笔记-分片广播机制详解.md) - 原理详解
- 🛠️ [XXL-JOB分片广播实战指南.md](./XXL-JOB文档/XXL-JOB分片广播实战指南.md) - 实战应用

详见：[XXL-JOB文档/XXL-JOB学习资料索引.md](./XXL-JOB文档/XXL-JOB学习资料索引.md)

### 3. 分块文件清理机制

- ✅ 定时清理过期分块文件
- ✅ 可配置清理策略
- ✅ 详细的统计信息

详见：[XXL-JOB文档/分块文件清理机制实现方案.md](./XXL-JOB文档/分块文件清理机制实现方案.md)

---

## 📚 学习资源

### XXL-JOB 学习文档（⭐强烈推荐）

本项目包含完整的 XXL-JOB 学习文档，共 **12 篇 markdown，约 15 万字**：

#### 快速入门
- [开始使用 XXL-JOB.md](./XXL-JOB文档/开始使用XXL-JOB.md)
- [XXL-JOB 快速启动指南.md](./XXL-JOB文档/XXL-JOB快速启动指南.md)
- [快速开始-分片广播测试.md](./XXL-JOB文档/快速开始-分片广播测试.md)

#### 理论学习
- [视频笔记-分片广播机制详解.md](./XXL-JOB文档/视频笔记-分片广播机制详解.md)

#### 实战操作
- [XXL-JOB 集成指南.md](./XXL-JOB文档/XXL-JOB集成指南.md)
- [XXL-JOB 分片广播实战指南.md](./XXL-JOB文档/XXL-JOB分片广播实战指南.md)
- [XXL-JOB 任务配置指南.md](./XXL-JOB文档/XXL-JOB任务配置指南.md)

**推荐从这里开始**：[XXL-JOB 学习资料索引.md](./XXL-JOB文档/XXL-JOB学习资料索引.md)

---

## 🎯 项目亮点

### 1. 完整的 XXL-JOB 分片广播实现

- ✅ 分片广播测试任务
- ✅ 动态扩容支持
- ✅ 详细的学习文档（15 万字）
- ✅ 从理论到实战的完整教程

### 2. 高性能大文件上传

- ✅ 分块上传
- ✅ 断点续传
- ✅ MD5 秒传
- ✅ 并发上传优化

### 3. 视频处理任务

- ✅ 分布式视频转码（框架已搭建）
- ✅ 分片广播实现并行处理
- ✅ 自动清理临时文件

---

## 📞 相关链接

- **GitHub 仓库**：https://github.com/2181025743/xuecheng-plus-project
- **视频教程**：[黑马程序员-学成在线](https://www.bilibili.com/video/BV1j8411N7Bm/)
- **XXL-JOB 官网**：https://www.xuxueli.com/xxl-job/

---

## 👨‍💻 开发者

- **作者**：yx
- **邮箱**：2181025743@qq.com
- **更新时间**：2025-10-09

---

## 📝 更新日志

### 2025-10-09
- ✨ 添加 XXL-JOB 完整学习文档（12 篇，15 万字）
- ✨ 实现分片广播测试任务
- ✨ 添加视频转码任务框架
- ✨ 实现分块文件清理机制
- 📁 整理 XXL-JOB 文档目录结构
- 🔐 移除敏感配置信息
- 📖 添加配置说明文档

### 之前版本
- ✨ 实现大文件分块上传功能
- ✨ 实现断点续传
- ✨ 集成 XXL-JOB
- ✨ 搭建项目基础架构

---

## ⚠️ 重要提醒

1. **配置敏感信息**：部署前请阅读 [配置说明-重要.md](./配置说明-重要.md)
2. **XXL-JOB 学习**：强烈推荐阅读 XXL-JOB 文档目录
3. **数据库初始化**：需要执行相关 SQL 脚本
4. **端口配置**：确保各服务端口不冲突

---

## 📄 License

本项目仅用于学习交流，请勿用于商业用途。

---

## 🙏 致谢

感谢黑马程序员提供的优质教程！

---

**Star ⭐ 如果这个项目对你有帮助！**

