# 短剧后台开发：一次线上 OOM 问题

## 一、问题背景

在短剧后台开发过程中，曾遇到一次线上 JVM OOM（Out Of Memory）问题。

典型现象：

- 后台接口出现大量超时
- JVM 堆内存持续上涨
- Full GC 频率越来越高
- Full GC 后 Old Gen 仍然维持较高占用
- 最终部分服务实例出现：

```text
java.lang.OutOfMemoryError: Java heap space
```

需要注意的是：**OOM 不一定意味着 JVM 堆配置太小，也可能是代码一次性持有了过多对象。**

---

## 二、问题排查思路

整体按照：

```text
线上异常
    ↓
查看 QPS / CPU / 内存
    ↓
判断是否流量突增
    ↓
查看 JVM GC
    ↓
jstat 分析堆内存
    ↓
Heap Dump
    ↓
MAT / JProfiler 分析
    ↓
查看对象引用链 / GC Root
    ↓
定位业务代码
    ↓
优化
    ↓
监控验证
```

---

## 三、第一步：排查是否流量突增

首先查看服务监控：

```text
QPS：正常
CPU：没有明显异常
堆内存：持续上涨
Full GC：频繁
GC 后 Old Gen：无法明显下降
```

如果 QPS 没有明显上涨，而堆内存却持续增长，就不能简单认为是流量问题。

这种情况更应该怀疑：

- 一次性查询大量数据
- List / Map 持有大量对象
- 本地缓存无限增长
- ThreadLocal 没有及时清理
- 静态集合长期持有对象
- 大对象进入老年代
- 业务代码存在内存泄漏

---

## 四、第二步：查看 JVM GC

可以使用：

```bash
jstat -gcutil <pid> 1s
```

重点观察：

- Eden
- Old Gen
- Full GC 次数
- Full GC 后 Old Gen 使用率

例如：

```text
Old Gen
60% → 70% → 80% → 90%
```

如果 Full GC 后 Old Gen 仍然保持较高水平，说明这些对象很可能仍然被程序引用，GC 无法回收。

---

## 五、第三步：导出 Heap Dump

可以使用：

```bash
jmap -dump:live,format=b,file=/tmp/heap.hprof <pid>
```

然后使用工具分析：

- MAT（Eclipse Memory Analyzer）
- JProfiler
- VisualVM

重点查看：

1. 哪些 Class 占用内存最多
2. 哪些对象数量异常
3. 对象为什么一直没有被回收
4. 谁在引用这些对象
5. 对应的 GC Root 是什么

核心思路：

```text
占用内存最多的对象
        ↓
查看引用关系
        ↓
找到 GC Root
        ↓
定位是谁一直持有对象
        ↓
回到业务代码
```

---

# 六、最终定位：后台统计接口一次性加载大量数据

最终定位到一个短剧后台数据统计接口。

原来的代码类似：

```java
List<Episode> episodes = episodeMapper.selectAll();

for (Episode episode : episodes) {
    // 统计播放量
}
```

问题在于：

随着短剧和剧集数据不断增长，`selectAll()` 会把大量数据一次性加载到 JVM 堆中。

如果后面还构建 Map：

```java
Map<Long, Episode> episodeMap = episodes.stream()
        .collect(Collectors.toMap(Episode::getId, e -> e));
```

那么 JVM 中可能同时存在：

```text
List
  ↓
大量 Episode 对象

Map
  ↓
大量 HashMap.Entry
  ↓
Episode 对象
```

同时还包含：

- String
- Integer / Long 等对象
- 业务 DTO
- 数据库查询结果
- 集合对象

因此实际内存占用可能远大于数据库中数据本身的大小。

---

# 七、解决方案

## 7.1 数据库分页

原来的方式：

```sql
SELECT * FROM episode;
```

改成分页查询：

```sql
SELECT id, drama_id, episode_no, title
FROM episode
WHERE drama_id = ?
ORDER BY episode_no
LIMIT ?, ?;
```

Java：

```java
Page<Episode> page = new Page<>(current, size);

IPage<Episode> result =
        episodeMapper.selectPage(page, wrapper);
```

这样 JVM 每次只处理有限数量的数据。

---

## 7.2 只查询需要的字段

如果统计播放量并不需要完整剧集信息，就不要：

```sql
SELECT *
```

例如只查询：

```sql
SELECT id, drama_id, episode_no
FROM episode
WHERE drama_id = ?;
```

减少：

- 网络传输
- MySQL 返回数据量
- JDBC 对象创建
- Java 堆内存占用

这是非常常见且有效的优化手段。

---

## 7.3 大数据量采用分批处理

如果确实需要处理几十万甚至上百万条数据，也不能简单：

```java
List<Data> list = mapper.selectAll();
```

可以采用分批读取：

```text
数据库
  ↓
读取 1000 条
  ↓
处理
  ↓
释放临时对象
  ↓
继续读取 1000 条
  ↓
处理
  ↓
...
```

例如：

```java
long lastId = 0;

while (true) {

    List<Episode> list =
            mapper.selectByIdRange(lastId, 1000);

    if (list.isEmpty()) {
        break;
    }

    for (Episode episode : list) {
        process(episode);
    }

    lastId = list.get(list.size() - 1).getId();
}
```

---

# 八、为什么大数据量更适合基于 ID 分批？

传统分页：

```sql
LIMIT 1000000, 1000
```

随着 offset 越来越大，数据库可能需要扫描、跳过大量记录。

更适合采用基于主键的方式：

```sql
SELECT id, drama_id, episode_no, title
FROM episode
WHERE id > #{lastId}
ORDER BY id
LIMIT 1000;
```

处理完成后：

```text
lastId = 当前批次最后一条数据的 ID
```

下一次继续：

```text
id > lastId
```

这种方式通常称为：

- Seek Pagination
- Keyset Pagination
- 基于游标/主键的分页

对于超大数据量处理更加合适。

---

# 九、如果 Heap Dump 发现是缓存导致的 OOM

另一类常见问题是本地缓存。

例如：

```java
Map<String, Object> cache = new HashMap<>();

while (...) {
    cache.put(key, value);
}
```

如果没有淘汰机制，数据会持续增长。

Heap Dump 中可能看到大量：

```text
HashMap
ArrayList
byte[]
char[]
String
```

这时需要检查：

- 是否存在无限增长的 HashMap
- 是否使用了静态集合
- 是否本地缓存没有 TTL
- 是否缓存没有最大容量
- 是否存在失效数据没有清理

可以考虑：

```text
Caffeine
Redis
```

并设置：

- 最大容量
- TTL
- 淘汰策略

---

# 十、为什么 Full GC 也回收不掉？

这是 OOM 面试中的重点问题。

如果对象仍然能够从 GC Root 到达：

```text
GC Root
  ↓
业务对象
  ↓
List / Map
  ↓
大量 Episode
```

那么 JVM 会认为这些对象仍然“有用”，不会回收。

所以：

> Full GC 后 Old Gen 仍然很高，并不一定代表 GC 失效，而可能是大量对象仍然被业务代码引用。

---

# 十一、为什么不能简单增加 -Xmx？

例如：

```bash
-Xms2g
-Xmx4g
```

虽然可以延缓 OOM，但是不能解决根因。

如果代码仍然：

```java
selectAll();
```

那么随着数据量继续增长：

```text
2GB
 ↓
4GB
 ↓
8GB
 ↓
OOM
```

而且堆越大，Full GC 的潜在成本也可能增加。

正确思路应该是：

```text
先定位问题
    ↓
减少对象创建
    ↓
减少对象持有时间
    ↓
分页 / 分批处理
    ↓
优化缓存
    ↓
必要时调整 JVM
```

---

# 十二、最终优化后的架构思路

```text
短剧后台
   ↓
Controller
   ↓
Service
   ↓
分页 / 分批查询
   ↓
MySQL
   ↓
每批处理有限数据
   ↓
统计结果
   ↓
Redis / MySQL
```

对于大规模统计任务，可以进一步采用：

```text
后台管理
   ↓
提交统计任务
   ↓
RocketMQ
   ↓
异步 Worker
   ↓
分批读取 MySQL
   ↓
处理数据
   ↓
写入统计结果
```

这样可以避免用户请求线程直接处理几十万甚至上百万数据。

---

# 十三、监控与预防

问题解决后，还应该增加监控，而不是只解决一次 OOM。

重点监控：

## JVM

- Heap Used
- Heap Max
- Old Gen
- Young GC
- Full GC
- GC Pause

## 接口

- QPS
- RT
- P95
- P99
- 错误率

## 服务

- CPU
- 内存
- Load
- 实例重启次数

如果使用 Spring Boot，可以结合：

```text
Spring Boot Actuator
        ↓
Micrometer
        ↓
Prometheus
        ↓
Grafana
```

查看 JVM 和接口指标。

分布式链路问题则可以结合：

```text
SkyWalking
```

定位：

```text
Gateway
  ↓
后台服务
  ↓
MySQL
  ↓
Redis
  ↓
ES
```

到底是哪一层耗时。

---

# 十四、面试标准回答

> 我们之前短剧后台遇到过一次线上 OOM。
>
> 当时主要现象是后台接口出现超时，同时 JVM 堆内存持续上涨，Full GC 越来越频繁，但是 Full GC 之后 Old Gen 仍然维持在比较高的水平，最后部分实例出现了 `Java heap space`。
>
> 我们首先通过监控排查 QPS、CPU 和内存情况，发现 QPS 没有明显突增，所以初步排除了单纯流量上涨导致的问题。
>
> 然后通过 `jstat` 查看 JVM GC 情况，并对异常实例导出 Heap Dump，使用 MAT 分析对象占用和 GC Root。最后定位到一个后台数据统计接口存在一次性查询大量剧集数据的问题。
>
> 原来的代码会把大量数据一次性查询成 List，后续统计过程中还会构建 Map，导致 JVM 中同时持有大量业务对象和集合对象。随着数据量增长，最终造成堆内存持续上涨。
>
> 后面我们主要做了几个优化：第一，把一次性查询改成分页查询；第二，只查询业务真正需要的字段；第三，对于几十万甚至上百万数据采用基于主键的分批处理，避免一次性加载到 JVM；第四，对一些高频统计数据增加缓存，减少数据库压力。
>
> 同时我们增加了 JVM、GC、接口 P95/P99 等监控和告警。
>
> 优化之后，单次任务的 JVM 内存占用明显下降，Full GC 次数也降低了。
>
> 这个问题让我比较深刻的一点是，OOM 不一定是 JVM 堆配置太小，很多时候真正的问题是业务代码一次性持有了过多对象。所以排查 OOM 时，我一般会结合 GC 指标、Heap Dump、GC Root 和业务代码一起定位，而不是直接去调大 `-Xmx`。

---

# 十五、面试官可能继续追问

### Q1：为什么 Full GC 之后内存还是很高？

因为这些对象仍然被 GC Root 引用，属于存活对象，GC 无法回收。

### Q2：Heap Dump 怎么定位问题？

先看对象占用排名，再分析对象引用链和 GC Root，最终回到业务代码定位 List、Map、缓存或者线程变量等长期持有对象。

### Q3：为什么不直接扩大 JVM 堆？

扩大堆只能延缓问题。如果根因是数据一次性加载或者对象长期被引用，数据继续增长后仍然会 OOM。

### Q4：为什么不用 `LIMIT 1000000,1000`？

大 offset 分页在大数据量下可能需要扫描并跳过大量数据，因此更适合使用基于主键的 Keyset/Seek Pagination。

### Q5：如果数据量达到千万级怎么办？

不能在同步 HTTP 请求里处理。可以将任务异步化：

```text
HTTP
 ↓
任务表
 ↓
RocketMQ
 ↓
Worker
 ↓
分批读取
 ↓
分批计算
 ↓
保存结果
```

### Q6：如果是 Redis 导致的内存问题呢？

需要区分 JVM OOM 和 Redis 内存问题。如果是 JVM 本地缓存，重点检查集合是否无限增长、是否存在 TTL/淘汰机制；如果是 Redis，则查看 Redis 内存指标、BigKey、Key 数量和淘汰策略。

---

# 十六、核心知识点总结

```text
线上 OOM
│
├── 1. 看 QPS / CPU / 内存
│
├── 2. jstat 查看 GC
│
├── 3. 判断 Full GC 后 Old Gen 是否下降
│
├── 4. Heap Dump
│
├── 5. MAT 分析对象
│
├── 6. 查看 GC Root
│
├── 7. 定位业务代码
│
├── 8. 分页 / 分批
│
├── 9. 减少查询字段
│
├── 10. 大数据量异步化
│
└── 11. JVM + 接口监控
```

**最核心的一句话：**

> OOM 排查不是简单地“把 JVM 内存调大”，而是要找到“什么对象占用了内存、为什么这些对象没有被回收、是谁一直持有它们”，然后从业务代码层面解决。
