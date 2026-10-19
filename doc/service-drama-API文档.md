# service-drama API 文档（短剧服务）

> 服务名：`service-drama`　端口：**8200**
> 基础路径：`/api/drama`
> 统一响应：`Result<T>`（`code` / `message` / `data`）
> 数据来源：MySQL（`bunny_flix`）+ ID 生成服务（Feign）+ RocketMQ（同步搜索）

---

## 一、数据传递链路

### 1.1 整体链路

```
┌──────────────┐
│   前端/调用方 │  page-bunny-mobile(5174) / page-bunnuy-admin / 其他微服务
└──────┬───────┘
       │ HTTP  /api/drama/**
       ▼
┌──────────────────────────────────────────────┐
│  API Gateway (8080)                          │
│  - AuthGlobalFilter：校验 JWT                 │
│  - 白名单：/api/search、/api/recommend/hot    │
│    ⚠️ /api/drama 不在白名单 → 需带 Token      │
│  - 解析出 userId/phone → 注入 X-User-Id 请求头 │
│  - 路由：Path=/api/drama/** → lb://service-drama │
└──────┬───────────────────────────────────────┘
       │  lb://service-drama（Nacos 服务发现 + 负载均衡）
       ▼
┌──────────────────────────────────────────────┐
│  service-drama (8200)                        │
│  Controller → Service → Mapper(MyBatis-Plus) │
└──────┬───────────────────┬───────────────────┘
       │                   │
       ▼                   ▼
   MySQL 读写        RocketMQ: drama-sync
  (t_drama 等)              │
                            ▼
                   service-search 消费
                    → 更新 Elasticsearch 索引
                    → 失效相关缓存
```

### 1.2 写入链路（以创建短剧为例）

```
POST /api/drama
  │
  ▼ DramaController.create()
DramaServiceImpl.createDrama()
  ├─1 调 ID 生成服务取全局唯一 ID
  │    IdGeneratorFeignClient.nextId(BizTypeEnum.DRAMA)
  │    → lb://service-id-generator /api/idgen/next
  │
  ├─2 写入 MySQL t_drama
  │
  └─3 发送同步消息（最终一致）
       DramaSyncProducer.sendDramaSync(DramaDTO)
       → RocketMQ topic: drama-sync
            ▼
       service-search 的 DramaSyncConsumer 消费
         ├─ 版本号校验（防乱序）
         ├─ SETNX 去重（防重复投递）
         ├─ 写入 ES 索引 drama
         └─ 失效 Redis 缓存 search:q:*、ranking
```

### 1.3 读取链路（以分页列表为例）

```
GET /api/drama/list?page=1&pageSize=18&status=1
  │
  ▼ DramaController.list()
DramaServiceImpl.listDramas()
  ├─ 构造 LambdaQueryWrapper
  │    status = ? / payType = ? / title like ? 
  │    tagId → 先查 t_drama_tag 得 dramaId 集合 → in 查询
  │    order by create_time desc
  ├─ MyBatis-Plus 分页插件执行
  └─ 返回 Page<Drama>（records / total / pages / current / size）
```

### 1.4 分类（标签）筛选链路

```
GET /api/drama/tag/all        →  t_tag 表，返回全部标签
GET /api/drama/list?tagId=5   →  t_drama_tag 查 dramaId → t_drama in 查询
```

**注意**：`tagId` 参数早期版本接收后未使用，已在 `DramaServiceImpl.listDramas()` 修复。

---

## 二、短剧接口（DramaController）

基础路径：`/api/drama`

| # | 方法 | 路径 | 说明 | 是否需要鉴权 |
|---|---|---|---|---|
| 1 | GET | `/list` | 分页列表（支持关键词/状态/付费类型/标签筛选） | 是 |
| 2 | GET | `/ranking` | **热门榜单**（按播放量或最新） | 是 |
| 3 | GET | `/{id}` | 短剧详情 | 是 |
| 4 | GET | `/{id}/exists` | 判断短剧是否存在 | 是 |
| 5 | POST | `` (即 `/api/drama`) | 创建短剧 | 是 |
| 6 | PUT | `/{id}` | 更新短剧 | 是 |
| 7 | DELETE | `/{id}` | 删除短剧（逻辑删除） | 是 |
| 8 | PUT | `/{id}/status` | 更新上下架状态 | 是 |
| 9 | PUT | `/{id}/completed` | 更新完结状态 | 是 |

### 1. 分页列表 `GET /api/drama/list`

**请求参数（Query）**

| 参数 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `page` | Integer | 否 | 1 | 页码，从 1 开始 |
| `pageSize` | Integer | 否 | 10 | 每页条数 |
| `keyword` | String | 否 | — | 标题模糊匹配 |
| `status` | Integer | 否 | — | 状态：0下架 1上架 |
| `payType` | Integer | 否 | — | 付费类型 |
| `tagId` | Long | 否 | — | 标签/分类ID（查关联表） |

**返回** `Result<Page<Drama>>`
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [ { "id": 1001, "title": "重生之逆袭人生", "cover": "...", "views": 98000 } ],
    "total": 120,
    "size": 18,
    "current": 1,
    "pages": 7
  }
}
```

**前端用法**（首页每行 6 个、初始 18 个、滚动加载）：
```
GET /api/drama/list?page=1&pageSize=18&status=1
```

---

### 2. 热门榜单 `GET /api/drama/ranking`

**请求参数**

| 参数 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `size` | Integer | 否 | 10 | 返回条数，最大 100 |
| `sortBy` | String | 否 | hot | `hot`=按播放量，`newest`=按最新 |

**返回** `Result<List<Drama>>`

**说明**：只返回 `status=1`（已上架）的短剧。

---

### 3. 短剧详情 `GET /api/drama/{id}`

**路径参数**：`id` = 短剧ID
**返回** `Result<Drama>`；不存在时返回 `code=404`

---

### 5. 创建短剧 `POST /api/drama`

**请求体** `Drama`（JSON）
```json
{
  "title": "重生之逆袭人生",
  "cover": "https://cdn.xxx.com/cover.jpg",
  "description": "讲述主角重生后逆袭的故事",
  "status": 1,
  "completed": 0,
  "payType": 0,
  "price": 0,
  "totalEpisodes": 24,
  "publisherId": 1
}
```
**返回** `Result<Drama>`
**副作用**：ID 由 ID 生成服务分配；发送 `drama-sync` 消息同步到搜索服务。

---

### 8. 更新状态 `PUT /api/drama/{id}/status?status=1`

上架/下架，会触发索引同步。

---

## 三、剧集接口（EpisodeController）

基础路径：`/api/drama/episode`

| # | 方法 | 路径 | 说明 |
|---|---|---|---|
| 1 | GET | `/list/{dramaId}` | **某短剧的全部剧集列表** |
| 2 | GET | `/{id}` | 剧集详情 |
| 3 | POST | `` | 创建剧集 |
| 4 | PUT | `/{id}` | 更新剧集 |
| 5 | DELETE | `/{id}` | 删除剧集 |
| 6 | PUT | `/{id}/sort?sortOrder=N` | 更新排序 |
| 7 | PUT | `/{id}/free?free=N` | 更新是否免费 |

### 剧集列表 `GET /api/drama/episode/list/{dramaId}`

**返回** `Result<List<Episode>>`
```json
{
  "code": 200,
  "data": [
    { "id": 20001, "dramaId": 1001, "title": "第1集", "episodeNum": 1,
      "cover": "...", "videoUrl": "...", "duration": 120, "free": 1,
      "enabled": 1, "sortOrder": 1 }
  ]
}
```

**前端用法**：详情页选集 —— 先取剧集列表，用户点某集后跳播放页 `/play/{episodeId}`。

---

## 四、标签/分类接口（TagController）

基础路径：`/api/drama/tag`

| # | 方法 | 路径 | 说明 |
|---|---|---|---|
| 1 | GET | `` (即 `/api/drama/tag`) | 标签列表；传 `parentId` 查子标签，不传查全部 |
| 2 | GET | `/all` | **全部标签**（前端分类栏用这个） |
| 3 | GET | `/{id}` | 标签详情 |
| 4 | POST | `` | 创建标签 |
| 5 | PUT | `/{id}` | 更新标签 |
| 6 | DELETE | `/{id}` | 删除标签 |

**返回** `Result<List<Tag>>` / `Result<Tag>`

---

## 五、演员接口（ActorController）

基础路径：`/api/drama/actor`

| # | 方法 | 路径 | 说明 |
|---|---|---|---|
| 1 | GET | `/list` | 分页列表，参数 `page`/`pageSize`/`keyword` |
| 2 | GET | `/{id}` | 演员详情 |
| 3 | POST | `` | 创建 |
| 4 | PUT | `/{id}` | 更新 |
| 5 | DELETE | `/{id}` | 删除 |

---

## 六、发行方接口（PublisherController）

基础路径：`/api/drama/publisher`

| # | 方法 | 路径 | 说明 |
|---|---|---|---|
| 1 | GET | `/list` | 分页列表，参数 `page`/`pageSize`/`keyword` |
| 2 | GET | `/{id}` | 详情 |
| 3 | POST | `` | 创建 |
| 4 | PUT | `/{id}` | 更新 |
| 5 | DELETE | `/{id}` | 删除 |

---

## 七、数据模型

### Drama（短剧表 `t_drama`）

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | Long | 主键（ID 生成服务分配） |
| `title` | String | 剧名 |
| `cover` | String | 封面图 URL |
| `description` | String | 简介 |
| `status` | Integer | 状态：0下架 1上架 |
| `completed` | Integer | 是否完结：0连载 1完结 |
| `payType` | Integer | 付费类型 |
| `price` | Integer | 价格 |
| `totalEpisodes` | Integer | 总集数 |
| `publisherId` | Long | 发行方ID |
| `views` | Long | 播放量 |
| `likes` | Long | 点赞数 |
| `collects` | Long | 收藏数 |
| `createTime` / `updateTime` | LocalDateTime | 时间 |
| `deleted` | Integer | 逻辑删除：0未删 1已删 |

### Episode（剧集表 `t_episode`）

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | Long | 主键 |
| `dramaId` | Long | 所属短剧ID |
| `title` | String | 集标题 |
| `episodeNum` | Integer | 第几集 |
| `cover` | String | 集封面 |
| `videoUrl` | String | 视频地址 |
| `duration` | Integer | 时长（秒） |
| `free` | Integer | 是否免费 |
| `enabled` | Integer | 是否启用 |
| `sortOrder` | Integer | 排序号 |

### 其它表

- `t_tag`：标签/分类
- `t_actor`：演员
- `t_publisher`：发行方
- `t_drama_tag`：短剧-标签关联（多对多）—— **分类筛选依赖此表**
- `t_drama_actor`：短剧-演员关联

---

## 八、Feign 契约（其他服务调用 service-drama）

`common-feign` 提供 `DramaFeignClient`：

```java
@FeignClient(name = "service-drama", path = "/api/drama")
public interface DramaFeignClient {
    Result<DramaDTO> getDramaById(Long id);
    Result<Boolean> dramaExists(Long id);
    Result<EpisodeDTO> getEpisodeById(Long episodeId);
    Result<List<EpisodeDTO>> listEpisodes(Long dramaId);
    Result<List<TagDTO>> listAllTags();
    Result<List<TagDTO>> listTagsByParentId(Long parentId);
}
```

**调用方**：
- `service-video`（播放服务）：查剧集信息用于播放鉴权与地址解析
- `service-search`（搜索服务）：消费 `drama-sync` 同步索引
- 其他业务服务按需引用

> Feign 走服务名直连（Nacos + LoadBalancer），**不经过 Gateway**，无需 token，但下游 `UserContext.getUserId()` 为 null。

---

## 九、消息契约

### Topic：`drama-sync`

**生产者**：`service-drama` 的 `DramaSyncProducer`
**消息体**：`DramaDTO`（JSON）
**触发时机**：短剧创建 / 更新 / 删除 / 上下架

**消费者**：`service-search` 的 `DramaSyncConsumer`
**消费逻辑**：
1. 版本号校验（`updateTime` 比对），丢弃乱序旧消息
2. Redis SETNX 去重，拦截重复投递
3. 写入/删除 Elasticsearch `drama` 索引
4. 失效 Redis 缓存（`search:q:*`、排行榜）
5. 失败不抛异常（避免无限重试导致堆积）

---

## 十、调用示例

### 前端（page-bunny-mobile）

```js
// 首页：初始 18 个
GET /api/drama/list?page=1&pageSize=18&status=1

// 下滑加载第 2 页
GET /api/drama/list?page=2&pageSize=18&status=1

// 分类筛选
GET /api/drama/tag/all
GET /api/drama/list?page=1&pageSize=18&status=1&tagId=5

// 榜单
GET /api/drama/ranking?size=20&sortBy=hot

// 详情 + 选集
GET /api/drama/1001
GET /api/drama/episode/list/1001
```

### cURL

```bash
# 需要登录（/api/drama 不在网关白名单）
TOKEN="你的JWT"

curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/drama/list?page=1&pageSize=18&status=1"

curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/drama/ranking?size=10&sortBy=hot"
```

### 直连服务（跳过网关，无需 token，调试用）

```bash
curl "http://localhost:8200/api/drama/list?page=1&pageSize=18"
```

---

## 十一、注意事项

1. **鉴权**：`/api/drama/**` **不在网关白名单**，经网关访问必须携带 `Authorization: Bearer <token>`，否则返回 401。
2. **ID 来源**：短剧/剧集等主键由 `service-id-generator` 分配，需保证该服务可用。
3. **分类筛选**：依赖 `t_drama_tag` 关联表；若标签下无短剧会返回空分页（不会报错）。
4. **索引最终一致**：短剧变更后 ES 索引通过 MQ 异步更新，存在短暂延迟（秒级）。
5. **逻辑删除**：`Drama.deleted` 字段，查询时 MyBatis-Plus 自动过滤。

---

*文档生成时间：2026-09-08*
*服务端口：8200　网关：8080*
