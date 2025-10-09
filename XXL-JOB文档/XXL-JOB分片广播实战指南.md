# XXL-JOB 分片广播实战指南

## 📚 分片广播核心原理

### 为什么需要分片广播？

**场景**：100 个视频需要转码，有 3 个执行器

❌ **普通路由策略的问题**（第一个、轮询、随机等）：

```
每次调度只有1个执行器工作
- 第1次调度：执行器1 处理 1个视频
- 第2次调度：执行器2 处理 1个视频
- 第3次调度：执行器3 处理 1个视频
→ 需要100次调度，其他执行器大部分时间在闲置
```

✅ **分片广播的优势**：

```
每次调度所有执行器同时工作
- 调度1次：
  * 执行器1（序号0）：处理 id%3=0 的视频（约33个）
  * 执行器2（序号1）：处理 id%3=1 的视频（约33个）
  * 执行器3（序号2）：处理 id%3=2 的视频（约34个）
→ 只需1次调度，3个执行器并行处理，效率提升3倍！
```

---

## 🔑 两个关键参数

调度中心在分片广播时，会给每个执行器分配两个参数：

```java
// 参数1：当前执行器的序号（从0开始编号）
int shardIndex = XxlJobHelper.getShardIndex();

// 参数2：执行器总数
int shardTotal = XxlJobHelper.getShardTotal();
```

**示例**：

```
3个执行器启动后：
- 执行器1：shardIndex=0, shardTotal=3
- 执行器2：shardIndex=1, shardTotal=3
- 执行器3：shardIndex=2, shardTotal=3
```

**如何避免重复处理？**

```sql
-- 执行器1（shardIndex=0）执行的SQL
SELECT * FROM media_process
WHERE status = '待转码'
AND MOD(id, 3) = 0  -- id能被3整除的（0,3,6,9...）
LIMIT 100;

-- 执行器2（shardIndex=1）执行的SQL
SELECT * FROM media_process
WHERE status = '待转码'
AND MOD(id, 3) = 1  -- id除以3余1的（1,4,7,10...）
LIMIT 100;

-- 执行器3（shardIndex=2）执行的SQL
SELECT * FROM media_process
WHERE status = '待转码'
AND MOD(id, 3) = 2  -- id除以3余2的（2,5,8,11...）
LIMIT 100;
```

通过取模运算，确保每个执行器处理不同的数据，**互不重复，互不遗漏**！

---

## 🚀 启动多个执行器实例

### 方法一：IDEA 启动多实例（推荐用于开发测试）

#### 步骤 1：检查配置文件

确保 `nacos/media-service-dev.yaml` 中配置了本地优先：

```yaml
spring:
  cloud:
    config:
      override-none: true # 本地配置优先于Nacos配置
```

#### 步骤 2：创建第一个启动配置（默认实例）

1. 打开 IDEA 运行配置
2. 找到 `MediaApplication` 启动类
3. 默认配置无需修改：
   - 端口：`63050`（application.yml 默认端口）
   - 执行器端口：`9999`（Nacos 配置的默认端口）

#### 步骤 3：创建第二个启动配置

1. **复制**现有的 MediaApplication 配置
2. 重命名为：`MediaApplication-9998`
3. 添加 VM 参数（Program arguments 或 VM options）：

**在 VM options 中添加**：

```
-Dserver.port=63051 -Dxxl.job.executor.port=9998
```

或者**在 Program arguments 中添加**：

```
--server.port=63051 --xxl.job.executor.port=9998
```

4. 点击 Apply → OK

#### 步骤 4：创建第三个启动配置（可选）

1. 再次复制配置
2. 重命名为：`MediaApplication-9997`
3. VM 参数：

```
-Dserver.port=63052 -Dxxl.job.executor.port=9997
```

#### 步骤 5：启动所有实例

1. 启动 `MediaApplication`（默认 63050 端口）
2. 启动 `MediaApplication-9998`
3. 启动 `MediaApplication-9997`（可选）

---

### 方法二：命令行启动（生产环境）

#### 1. 打包项目

```bash
mvn clean package -DskipTests
```

#### 2. 启动多个实例

```bash
# 第一个节点（默认配置）
java -jar xuecheng-plus-media-api.jar \
  --server.port=63050 \
  --xxl.job.executor.port=9999

# 第二个节点
java -jar xuecheng-plus-media-api.jar \
  --server.port=63051 \
  --xxl.job.executor.port=9998

# 第三个节点
java -jar xuecheng-plus-media-api.jar \
  --server.port=63052 \
  --xxl.job.executor.port=9997
```

---

## ✅ 验证执行器是否在线

### 1. 访问 XXL-JOB 调度中心

```
URL: http://127.0.0.1:8080/xxl-job-admin
用户名: admin
密码: 123456
```

### 2. 查看执行器列表

进入：**执行器管理** → 找到 `media-service-executor`

应该看到：

```
在线机器地址：
127.0.0.1:9999  ✅
127.0.0.1:9998  ✅
127.0.0.1:9997  ✅（如果启动了第三个）
```

**如果只看到一个或零个**：

- ❌ 检查执行器端口是否冲突
- ❌ 检查服务是否真的启动成功
- ❌ 检查防火墙或网络配置

---

## 📋 配置分片广播任务

### 步骤 1：新增任务

进入 **任务管理** → **新增**

#### 基础配置

| 配置项   | 值                       | 说明           |
| -------- | ------------------------ | -------------- |
| 执行器   | `media-service-executor` | 必须选择执行器 |
| 任务描述 | `分片广播测试任务`       | 随意填写       |
| 负责人   | `admin`                  | 随意填写       |

#### 调度配置

| 配置项   | 值              | 说明             |
| -------- | --------------- | ---------------- |
| 调度类型 | `CRON`          | 使用 CRON 表达式 |
| Cron     | `0/5 * * * * ?` | 每 5 秒执行一次  |

#### 任务配置 ⭐ 核心 ⭐

| 配置项       | 值                   | 说明                            |
| ------------ | -------------------- | ------------------------------- |
| 运行模式     | `BEAN`               | 固定值                          |
| JobHandler   | `shardingJobHandler` | ⭐ 必须和代码中@XxlJob 注解一致 |
| **路由策略** | **`分片广播`**       | ⭐⭐⭐ 最关键的配置 ⭐⭐⭐      |

#### 高级配置

| 配置项       | 值         | 说明         |
| ------------ | ---------- | ------------ |
| 阻塞处理策略 | `单机串行` | 推荐         |
| 调度过期策略 | `忽略`     | 测试时推荐   |
| 失败重试次数 | `0`        | 测试时不重试 |

### 步骤 2：启动任务

保存后，在任务列表中点击 **启动** 按钮。

### 步骤 3：查看执行日志

点击任务后面的 **执行日志** 按钮，应该看到：

```
执行器1的日志：
分片参数 → 当前分片序号: 0, 总分片数: 2
【执行器 0】开始处理属于自己的数据分片...
【执行器 0】处理逻辑：查询 id % 2 = 0 的数据

执行器2的日志：
分片参数 → 当前分片序号: 1, 总分片数: 2
【执行器 1】开始处理属于自己的数据分片...
【执行器 1】处理逻辑：查询 id % 2 = 1 的数据
```

**关键观察点**：

- ✅ 两个执行器同时执行（时间戳几乎相同）
- ✅ shardIndex 不同（一个是 0，一个是 1）
- ✅ shardTotal 相同（都是 2）

---

## 🔬 动态扩容测试

### 什么是动态扩容？

当你启动新的执行器时，XXL-JOB 会**自动识别**并调整分片参数。

### 测试步骤

1. **初始状态**：2 个执行器在线

   ```
   执行器1: shardIndex=0, shardTotal=2
   执行器2: shardIndex=1, shardTotal=2
   ```

2. **启动第 3 个执行器**（端口 9997）

   ```bash
   -Dserver.port=63052 -Dxxl.job.executor.port=9997
   ```

3. **观察日志变化**：下一次调度时

   ```
   执行器1: shardIndex=0, shardTotal=3  ← shardTotal自动变成3
   执行器2: shardIndex=1, shardTotal=3
   执行器3: shardIndex=2, shardTotal=3  ← 新加入的
   ```

4. **关闭第 3 个执行器**：再下一次调度
   ```
   执行器1: shardIndex=0, shardTotal=2  ← 自动恢复成2
   执行器2: shardIndex=1, shardTotal=2
   ```

**结论**：无需修改配置，执行器数量变化时自动调整分片！

---

## 🎯 实战应用：视频转码任务

### 数据库查询示例

假设 `media_process` 表存储待转码视频：

```java
@XxlJob("videoTranscodeJob")
public void videoTranscodeJob() {
    // 获取分片参数
    int shardIndex = XxlJobHelper.getShardIndex();
    int shardTotal = XxlJobHelper.getShardTotal();

    log.info("当前执行器：{}/{}，开始处理视频转码任务", shardIndex, shardTotal);

    // 查询当前分片应该处理的视频
    List<MediaProcess> videoList = mediaProcessMapper.selectList(
        new LambdaQueryWrapper<MediaProcess>()
            .eq(MediaProcess::getStatus, "待转码")
            .apply("MOD(id, {0}) = {1}", shardTotal, shardIndex)
            .last("LIMIT 100")
    );

    log.info("执行器 {} 查询到 {} 个待转码视频", shardIndex, videoList.size());

    // 依次处理每个视频
    for (MediaProcess video : videoList) {
        try {
            // 调用FFmpeg转码
            transcodeService.transcode(video);
            log.info("视频转码成功：{}", video.getFileName());
        } catch (Exception e) {
            log.error("视频转码失败：{}", video.getFileName(), e);
        }
    }

    XxlJobHelper.handleSuccess("执行器 " + shardIndex + " 处理完成");
}
```

### MyBatis-Plus 查询（推荐）

```java
// 方式1：使用apply方法
List<MediaProcess> list = mediaProcessMapper.selectList(
    new LambdaQueryWrapper<MediaProcess>()
        .eq(MediaProcess::getStatus, "待转码")
        .apply("MOD(id, {0}) = {1}", shardTotal, shardIndex)
        .orderByAsc(MediaProcess::getCreateDate)
        .last("LIMIT 100")
);
```

### XML Mapper 查询

```xml
<select id="selectBySharding" resultType="com.xuecheng.media.model.po.MediaProcess">
    SELECT * FROM media_process
    WHERE status = '待转码'
    AND MOD(id, #{shardTotal}) = #{shardIndex}
    ORDER BY create_date ASC
    LIMIT 100
</select>
```

---

## 📊 分片效果对比

### 场景：1000 个视频需要转码

#### 不使用分片广播（轮询策略）

```
假设每个视频转码需要10秒

执行器1：第1次处理1个视频，第4次处理1个，第7次...（需要处理约333个）
执行器2：第2次处理1个视频，第5次处理1个，第8次...（需要处理约333个）
执行器3：第3次处理1个视频，第6次处理1个，第9次...（需要处理约334个）

调度次数：1000次
总耗时：1000 × 10秒 = 10000秒 ≈ 2.78小时
```

#### 使用分片广播

```
调度1次，3个执行器同时工作：

执行器1：处理 id%3=0 的视频（约333个） → 3330秒
执行器2：处理 id%3=1 的视频（约333个） → 3330秒
执行器3：处理 id%3=2 的视频（约334个） → 3340秒

调度次数：1次
总耗时：max(3330, 3330, 3340) = 3340秒 ≈ 0.93小时
```

**效率提升**：2.78 小时 → 0.93 小时，**提升约 3 倍**！

---

## ⚠️ 常见问题

### 1. 执行器显示离线

**原因**：

- 端口冲突
- 网络不通
- executor 配置错误

**解决**：

```bash
# 检查端口是否被占用
netstat -ano | findstr 9999

# 检查日志
tail -f logs/media-api.log
```

### 2. 分片参数获取为 0

**原因**：路由策略没有选择"分片广播"

**解决**：

- 进入任务管理
- 编辑任务
- 路由策略改为：**分片广播（SHARDING_BROADCAST）**

### 3. 任务重复执行

**原因**：SQL 查询没有使用分片条件

**解决**：

```sql
-- ❌ 错误：没有分片条件
SELECT * FROM media_process WHERE status = '待转码'

-- ✅ 正确：添加MOD分片条件
SELECT * FROM media_process
WHERE status = '待转码'
AND MOD(id, #{shardTotal}) = #{shardIndex}
```

### 4. 只有一个执行器在工作

**原因**：

- 只启动了一个执行器实例
- 路由策略不是"分片广播"

**解决**：

- 检查执行器管理，确保多个在线
- 检查任务配置的路由策略

---

## 📖 总结

### 分片广播的核心要点

1. **何时使用**：

   - ✅ 需要处理大量数据
   - ✅ 数据可以分片（如按 ID 取模）
   - ✅ 希望多节点并行提升效率

2. **关键配置**：

   - ✅ 路由策略：**分片广播**
   - ✅ 阻塞策略：单机串行
   - ✅ 多个执行器在线

3. **代码实现**：

   - ✅ 获取 `shardIndex` 和 `shardTotal`
   - ✅ SQL 使用 `MOD(id, shardTotal) = shardIndex`
   - ✅ 每个执行器处理不同分片

4. **优势**：
   - ✅ 自动负载均衡
   - ✅ 动态扩容/缩容
   - ✅ 避免重复处理
   - ✅ 充分利用集群资源

---

## 🔗 相关文档

- `XXL-JOB任务配置指南.md` - 所有任务配置参考
- `XXL-JOB快速启动指南.md` - 快速启动教程
- `XXL-JOB集成指南.md` - 集成步骤详解
- `XXL-JOB集成完成总结.md` - 集成总结
