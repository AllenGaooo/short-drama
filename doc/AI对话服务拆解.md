# 短剧 AI 对话服务 —— 逐句拆解（流程 + 代码支撑）

> 描述原文拆为 4 句，每句给出：**业务含义 → 处理流程 → 代码支撑（精确到方法/行号）→ 面试话术**

---

## 全句总览

| # | 描述 | 核心服务 | 状态 |
|---|---|---|---|
| 1 | 基于 Spring AI 构建短剧对话 Agent，实现用户意图识别、短剧检索、短剧推荐及多轮上下文交互 | `bunny-flix-agent` | ⚠️ 手写编排（非 Spring AI，见第 1 句说明） |
| 2 | 基于 RAG 构建短剧知识库，将简介/角色/题材/标签向量化，实现语义检索 | `bunny-flix-rag` | ⚠️ 词频向量（非 Embedding 模型） |
| 3 | 保存多轮对话上下文，支持「上一部」「类似刚才那部」等指代表达 | `bunny-flix-agent` | ✅ 已实现 |
| 4 | 将搜索、详情、榜单封装为 Tool，由 Agent 按意图动态调用 | `bunny-flix-mcp` | ✅ 已实现（6 个工具） |

---

## 第 1 句

> **「基于 Spring AI 构建短剧对话 Agent，实现用户意图识别、短剧检索、短剧推荐及多轮上下文交互」**

### 业务含义
用户用自然语言描述需求（「推荐个都市逆袭的剧」），Agent 要理解意图、检索内容、给出推荐，并支持追问。

### 处理流程
```
POST /api/agent/chat {message, history}
  │
  ▼ AgentController.chat()
AgentService.chat()                       ← 编排入口（第 79 行）
  ├─0 上下文消解    resolveReference()     第 90 行
  ├─1 RAG 检索     retrieve()             第 153 行
  ├─2 意图识别+工具  decideAndCallTools()   第 167 行
  ├─3 记忆剧名      rememberDrama()        第 117 行
  └─4 生成回复      generate()             第 248 行
        ├─ 配了 LLM → callLlm()            第 301 行
        └─ 未配    → localReply()          第 261 行（本地兜底）
```

### 代码支撑

**① 意图识别（规则型）** — `AgentService.java` 第 64 行
```java
private static final Map<String, String> TOOL_KEYWORDS = new LinkedHashMap<>();
static {
    TOOL_KEYWORDS.put("推荐", "recommend_drama");
    TOOL_KEYWORDS.put("类似", "recommend_drama");
    TOOL_KEYWORDS.put("榜单", "get_hot_ranking");
    TOOL_KEYWORDS.put("热门", "get_hot_ranking");
    TOOL_KEYWORDS.put("详情", "get_drama_detail");
    TOOL_KEYWORDS.put("短剧", "query_drama");
    ...
}
```
`LinkedHashMap` 保证**匹配优先级**（"推荐"优先于"短剧"）。

**② 编排主体** — `AgentService.chat()` 第 79-115 行

**③ 生成回复** — `generate()` 第 248 行
- 开 LLM：`callLlm()` 第 301 行（OpenAI 兼容接口）
- 未开：`localReply()` 第 261 行（本地模板拼装，保证离线可跑）

### ⚠️ 与描述的差距 & 面试话术

**实际未使用 Spring AI**，是手写 ReAct 编排（`RestTemplate` + 规则匹配）。

> **面试说法**："Spring AI 的 `function calling` 确实能少写代码，但它绑定了特定模型抽象；而我们的检索源（RAG/图谱）和工具（MCP）都是独立微服务，手写编排让每一层都能单独替换、不绑死框架。"

**演进路径**（可当"后续优化"讲）：引入 `spring-ai-openai-spring-boot-starter`，把 6 个工具注册为 `Function` Bean，改用 `ChatClient.functions(...)` 让模型自己选工具。

---

## 第 2 句

> **「基于 RAG 构建短剧知识库，将短剧简介、角色、题材、标签等内容进行向量化，实现语义检索」**

### 业务含义
把短剧内容变成向量存起来，用户用自然语言问（不用精确关键词）也能匹配到相关短剧。

### 处理流程
```
服务启动 → RagService.init()          @PostConstruct，预置知识库（第 27 行）
                 │
                 ▼
知识入库 → ingest(docs)               第 39 行
                 │  每条 content 经 vectorize() 转向量
                 ▼
检索     → retrieve(query, topK)      第 51 行
                 │  query 向量化 → 与全库算余弦相似度 → 排序取 topK
                 ▼
返回 [{content, score}, ...]
```

### 代码支撑

**① 知识库初始化** — `RagService.init()` 第 27 行（`@PostConstruct`）
预置短剧平台知识（简介、会员体系、优惠券等 6 条）

**② 向量化** — `vectorize()` 第 75 行
```java
// 字符 2-gram 词频 → 哈希到 1024 维 → 余弦相似度
String clean = text.replaceAll("\\s+", "");
for (int i = 0; i < clean.length(); i++) {
    String gram = clean.substring(i, Math.min(i + 2, clean.length()));
    tf.merge(gram, 1, Integer::sum);   // 词频统计
}
// 哈希到 1024 维向量
```

**③ 语义检索** — `retrieve()` 第 51 行 + `cosine()` 第 93 行
```java
double[] qv = vectorize(query);
// 与每条知识算余弦相似度 → 降序 → 取 topK
```

**④ 入库接口** — `RagController`：`POST /api/rag/ingest`、`GET /api/rag/retrieve`

### ⚠️ 与描述的差距 & 面试话术

**向量化用的是「词频 + 哈希」，不是 Embedding 模型**。

> **面试说法**："向量检索我做了分层设计：内存版用 TF 词频 + 余弦相似度，保证不依赖任何外部组件也能跑通全链路；生产可平滑换成 Milvus + Embedding API——因为 `ingest`/`retrieve` 两个接口是稳定的，换实现不动上层。"

**升级路径**：
1. 引 `milvus-sdk-java`，`RagService` 内部改写入 Milvus collection
2. 加 `EmbeddingClient` 调 Embedding HTTP API
3. 切片用**结构化切分**（一条短剧简介 = 一个 chunk），保证召回语义完整

---

## 第 3 句

> **「保存多轮对话上下文，使用户能够使用『上一部』『类似刚才那部』等上下文表达进行连续交互」**

### 业务含义
用户会连续追问，用「上一部」指代前面提到的剧。必须把它**还原成具体剧名**才能检索。

### 处理流程
```
第1轮「有哪些热门短剧？」
  → 工具返回 → rememberDrama() 记录剧名《重生之逆袭人生》
                                        ↓
第2轮「类似刚才那部」
  → resolveReference() 命中"刚才那部"
  → 重写为「重生之逆袭人生 类似」
  → 用重写后的语句做 RAG + 工具调用    ← 关键：否则检索不到
```

### 代码支撑

**① 指代词表** — `ConversationContextService.java` 第 33-46 行
```java
REFERENCE_WORDS.put("上一部", "%s");
REFERENCE_WORDS.put("刚才那部", "%s");
REFERENCE_WORDS.put("那部", "%s");
REFERENCE_WORDS.put("这部", "%s");
REFERENCE_WORDS.put("类似的", "%s 类似");    // 相似推荐
REFERENCE_WORDS.put("再来一部", "%s 类似");
REFERENCE_WORDS.put("换一部", "%s 类似");
```
两类替换：直接替换剧名（`%s`）／追加"类似"（`%s 类似`）

**② 指代消解** — `resolveReference()` 第 62 行
```java
for (Map.Entry<String, String> entry : REFERENCE_WORDS.entrySet()) {
    if (message.contains(word)) {
        return message.replace(word, String.format(entry.getValue(), lastDrama));
    }
}
```

**③ 记忆剧名** — `recordDrama()` 第 93 行；`AgentService.rememberDrama()` 第 117 行（从工具结果提取首部剧名）

**④ 上下文存储** — 第 53 行 `STATE_TTL_MS = 30 分钟`
```java
private final Map<Long, ConversationState> states = new ConcurrentHashMap<>();
```
按用户维度存储，30 分钟无交互自动过期

**⑤ 调用位置** — `AgentService.chat()` 第 90 行
```java
String resolvedMessage = conversationContextService.resolveReference(contextKey, message);
// 后续 RAG 与工具都用 resolvedMessage，而不是原始 message
```

### 面试话术
> "多轮上下文的核心难点是**指代消解**——用户说『类似刚才那部』，检索层根本不认识『刚才那部』是什么。所以我在检索前加了一步：把指代词还原成上一轮提到的具体剧名。记忆的剧名来自工具返回结果，按用户维度存 30 分钟。"

**追问：为什么不用大模型做消解？**
→ 规则方案**零成本、零延迟、可解释**，覆盖高频表达已足够；长尾交给大模型兜底。这是成本与效果的权衡。

---

## 第 4 句

> **「将短剧搜索、剧集详情、热门榜单等业务能力封装为 Tool，由 Agent 根据用户意图动态调用」**

### 业务含义
把散落各处的业务能力抽象成统一工具，Agent 按意图自动选择调用哪个。

### 处理流程
```
AgentService.decideAndCallTools(message)     第 167 行
  │  遍历 TOOL_KEYWORDS，命中则调对应工具（单次最多 2 个）
  ▼
callTool(tool, message) → HTTP POST lb://bunny-flix-mcp /api/mcp/call
  ▼
McpToolService.call(name, args)              第 37 行（switch 分发）
```

### 代码支撑

**① 工具定义** — `McpToolService.listTools()` 第 19 行
**② 工具分发** — `McpToolService.call()` 第 37 行

**6 个工具清单**：

| 工具 | 方法位置 | 触发关键词 | 能力 |
|---|---|---|---|
| `query_drama` | 第 63 行 | 短剧 / 剧 | 关键词查短剧 |
| `recommend_drama` | 第 95 行 | 推荐 / 类似 | **相似推荐 + 热门推荐** |
| `get_drama_detail` | 第 134 行 | 详情 | 剧集详情（简介、集数、主演） |
| `get_hot_ranking` | 第 147 行 | 榜单 / 排行 / 热门 | 热门榜单 TopN |
| `query_user` | — | 用户 | 查用户 |
| `send_notice` | — | 通知 | 发站内通知 |

**③ 工具数据源** — `sampleDramas()` 第 53 行（5 部示例短剧，含题材/热度，用于推荐与榜单）

**④ 工具注册接口** — `McpController`：`GET /api/mcp/tools`（列出所有工具）、`POST /api/mcp/call`（调用）

**⑤ 动态调用决策** — `AgentService.decideAndCallTools()` 第 167 行
```java
for (Map.Entry<String, String> e : TOOL_KEYWORDS.entrySet()) {
    if (message.contains(e.getKey())) {
        calls.add(callTool(e.getValue(), message));
        if (calls.size() >= 2) break;   // 单次最多调 2 个工具
    }
}
```

### 面试话术
> "我把短剧搜索、详情、热门榜单、相似推荐封装成了 MCP 工具，每个工具有名称、描述、参数说明。Agent 按意图识别结果动态调用——现在是规则匹配，零成本且可解释；开了大模型之后可以升级成 function calling，让模型自己决定调哪个工具。"

---

## 五、一次完整对话的时序（串起 4 句话）

**用户输入**：「类似刚才那部」

| 步骤 | 对应第几句 | 代码 |
|---|---|---|
| 1. 接收请求 | — | `AgentController.chat()` |
| 2. 还原 `X-User-Id` | — | `AgentContextFilter` |
| 3. **指代消解**（"刚才那部"→"重生之逆袭人生"） | 第 3 句 | `resolveReference()` 第 62 行 |
| 4. **RAG 语义检索** | 第 2 句 | `retrieve()` 第 153 行 → `RagService.retrieve()` 第 51 行 |
| 5. **意图识别**（命中"类似"→ recommend_drama） | 第 1 句 | `decideAndCallTools()` 第 167 行 |
| 6. **工具调用** | 第 4 句 | `McpToolService.call()` 第 37 行 → `recommendDrama()` 第 95 行 |
| 7. 记录本轮剧名（供下次指代） | 第 3 句 | `rememberDrama()` 第 117 行 |
| 8. 生成回复 | 第 1 句 | `generate()` 第 248 行 |

**返回**：`{reply, usedTools, references, resolvedMessage, contextDrama}`

---

## 六、服务与端口

| 服务 | 端口 | 职责 |
|---|---|---|
| `bunny-flix-agent` | 8801 | 编排（意图/检索/工具/生成） |
| `bunny-flix-mcp` | 8802 | 工具服务（6 个业务能力） |
| `bunny-flix-rag` | 8803 | RAG 知识检索 |
| `bunny-flix-aigateway` | 8804 | 第二套编排（意图 → RAG/图谱） |
| `bunny-flix-graph` | 8805 | 知识图谱（关系查询） |
| `page-bunny-agent` | 5176 | 前端对话页（免登录） |

---

## 七、⚠️ 面试必记的两处诚实说明

| 描述 | 实际 | 安全说法 |
|---|---|---|
| "基于 Spring AI" | 手写编排，无 Spring AI 依赖 | "手写编排让每层可独立替换，不绑死框架；演进路径是接入 Spring AI 的 function calling" |
| "向量化" | TF 词频 + 哈希，非 Embedding 模型 | "内存版保证全链路可跑通；`ingest`/`retrieve` 接口稳定，可平滑换 Milvus + Embedding API" |

这两点如果被发现是"简历写了但没做"杀伤力很大；**主动承认 + 给出选型理由 + 演进路径**，反而显得有判断力。

---

*文档生成时间：2026-09-07*
*配套：`面试.md`（技术实现细节）、`业务背景.md`（业务驱动力）*
