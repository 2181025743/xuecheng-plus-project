# XXL-JOB 学习文档目录

本目录包含学成在线项目中 XXL-JOB 分布式任务调度的完整学习资料。

## 📖 快速开始

**推荐从这里开始** → [XXL-JOB 学习资料索引.md](./XXL-JOB学习资料索引.md)

索引文件提供了：

- ✅ 所有文档的导航和分类
- ✅ 3 条不同的学习路径（新手/有基础/针对性学习）
- ✅ 快速查找功能
- ✅ 常见问题快速链接

---

## 📚 文档列表

### 🚀 快速入门

1. [开始使用 XXL-JOB.md](./开始使用XXL-JOB.md) - 基本概念
2. [XXL-JOB 快速启动指南.md](./XXL-JOB快速启动指南.md) - 调度中心启动
3. [快速开始-分片广播测试.md](./快速开始-分片广播测试.md) - 5 分钟快速测试

### 📖 理论学习

4. [视频笔记-分片广播机制详解.md](./视频笔记-分片广播机制详解.md) - 原理详解

### 🛠️ 实战操作

5. [XXL-JOB 集成指南.md](./XXL-JOB集成指南.md) - 集成步骤
6. [XXL-JOB 分片广播实战指南.md](./XXL-JOB分片广播实战指南.md) - 实战应用
7. [XXL-JOB 任务配置指南.md](./XXL-JOB任务配置指南.md) - 配置参考

### 📝 总结与业务

8. [XXL-JOB 集成完成总结.md](./XXL-JOB集成完成总结.md) - 知识回顾
9. [分块文件清理机制实现方案.md](./分块文件清理机制实现方案.md) - 定时清理
10. [断点续传完整流程详解.md](./断点续传完整流程详解.md) - 断点续传
11. [临时文件位置说明.md](./临时文件位置说明.md) - 临时文件管理

---

## 🎯 推荐学习路径

### 新手路径（2-3 小时）

```
开始使用XXL-JOB → 快速启动指南 → 集成指南
→ 快速开始测试 → 视频笔记 → 分片广播实战指南
```

### 快速上手（1 小时）

```
快速启动指南 → 快速开始测试 → 视频笔记 → 分片广播实战指南
```

### 针对性学习

- 测试分片广播 → `快速开始-分片广播测试.md`
- 理解原理 → `视频笔记-分片广播机制详解.md`
- 查询配置 → `XXL-JOB任务配置指南.md`

---

## 💡 核心知识点

### 分片广播核心参数

```java
int shardIndex = XxlJobHelper.getShardIndex(); // 执行器序号（0开始）
int shardTotal = XxlJobHelper.getShardTotal(); // 执行器总数
```

### 分片查询 SQL

```sql
SELECT * FROM table_name
WHERE status = '待处理'
AND MOD(id, #{shardTotal}) = #{shardIndex}
LIMIT 100;
```

---

## 🔗 相关代码位置

**任务定义**：

```
xuecheng-plus-media/
  xuecheng-plus-media-service/
    src/main/java/com/xuecheng/media/job/
      MediaProcessJob.java
```

**配置文件**：

```
nacos/media-service-dev.yaml
xxl-job-2.3.1/xxl-job-admin/src/main/resources/application.properties
```

---

## 📞 需要帮助？

- 查看 [XXL-JOB 学习资料索引.md](./XXL-JOB学习资料索引.md) 获取完整导航
- 参考"快速查找"部分找到特定问题的解决方案
- 查阅"常见问题快速链接"获取问题排查指南

---

**最后更新**：2025-10-09  
**文档数量**：12 个  
**总字数**：约 15 万字
