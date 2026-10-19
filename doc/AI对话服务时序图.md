# AI 对话服务 —— 时序图

> 服务：`bunny-flix-agent`(8801) / `bunny-flix-mcp`(8802) / `bunny-flix-rag`(8803)
> / `bunny-flix-aigateway`(8804) / `bunny-flix-graph`(8805)
> 前端：`page-bunny-agent`(5176)
>
> 用支持 Mermaid 的 Markdown 编辑器（Typora / VSCode 插件 / GitHub）打开可直接渲染。

---

## 一、整体架构时序图（一次完整对话）

```mermaid
sequenceDiagram
    autonumber
    participant U as 用户
    participant F as 前端 page-bunny-agent
    participant G as API Gateway (8080)
    participant A as bunny-flix-agent (8801)
    participant Ctx as ConversationContextService
    participant R as bunny-flix-rag (8803)
    participant M as bunny-flix-mcp (8802)
    participant AI as Spring AI / 大模型

    U->>F: 输入「类似刚才那部」
    F->>G: POST /api/agent/chat {message, history}
    Note over G: /api/agent 在白名单<br/>免登录放行
    G->>A: 转发（lb://bunny-flix-agent）
    Note over A: AgentContextFilter<br/>还原 X-User-Id → UserContext

    A->>Ctx: resolveReference(userId, message)
    Ctx-->>A: 重写为「重生之逆袭人生 类似」

    par 并行获取上下文
        A->>R: GET /api/rag/retrieve?query=重写后语句
        R-->>A: 语义检索结果 topK
    and
        A->>M: POST /api/mcp/call（规则匹配工具）
        M-->>A: 工具执行结果
    end

    A->>Ctx: recordDrama(记录本轮剧名)
    Note over Ctx: 供下一轮「上一部」指代使用

    A->>AI: ChatClient + functions（function calling）
    Note over AI: 模型自主判断是否需要调工具<br/>并生成自然语言回复
    AI-->>A: 最终回复

    A-->>G: {reply, usedTools, references, contextDrama}
    G-->>F: 响应
    F-->>U: 展示回复
```

---

## 二、AgentService 内部处理时序（细化）

```mermaid
sequenceDiagram
    autonumber
    participant Ctrl as AgentController
    participant Svc as AgentService
    participant Ctx as ConversationContext
    participant Rag as RagService(远程)
    participant Mcp as McpToolService
    participant Gen as SpringAiAgentService

    Ctrl->>Svc: chat(message, history)

    %% 第 0 步：指代消解
    Svc->>Ctx: resolveReference(userId, message)
    alt 含指代词（上一部/那部/类似的）
        Ctx-->>Svc: 还原为具体剧名
    else 无指代词
        Ctx-->>Svc: 原样返回
    end

    %% 第 1 步：RAG 检索
    Svc->>Rag: retrieve(resolvedMessage)
    alt 检索成功
        Rag-->>Svc: references
    else 失败
        Rag-->>Svc: 空列表（降级不阻断）
    end

    %% 第 2 步：工具调用
    Svc->>Svc: decideAndCallTools（关键词匹配）
    alt 命中关键词
        Svc->>Mcp: callTool(工具名, 参数)
        Mcp-->>Svc: 工具结果
    else 未命中
        Svc->>Svc: 不调工具
    end

    %% 第 3 步：记录上下文
    Svc->>Ctx: recordDrama(提取首部剧名)

    %% 第 4 步：生成回复（三级降级）
    Svc->>Gen: chat(message, history)
    alt Spring AI 可用
        Gen-->>Svc: 模型回复
    else Spring AI 失败/未配置
        Note over Svc: 降级到原生 LLM 接口
        alt 原生 LLM 可用
            Svc-->>Svc: callLlm()
        else 都不可用
            Svc-->>Svc: localReply() 本地模板
        end
    end

    Svc-->>Ctrl: 组装响应并返回
```

---

## 三、多轮上下文时序（体现「上一部」）

```mermaid
sequenceDiagram
    autonumber
    participant U as 用户
    participant A as AgentService
    participant Ctx as ConversationContext
    participant M as MCP 工具

    Note over U,M: ===== 第 1 轮 =====
    U->>A: 「有哪些热门短剧？」
    A->>Ctx: resolveReference（无指代词）
    Ctx-->>A: 原句
    A->>M: query_drama / get_hot_ranking
    M-->>A: [《重生之逆袭人生》, 《闪婚老公是首富》...]
    A->>Ctx: recordDrama("重生之逆袭人生")
    Note over Ctx: 上下文记录：lastDramaTitle=重生之逆袭人生<br/>TTL 30 分钟
    A-->>U: 推荐结果

    Note over U,M: ===== 第 2 轮 =====
    U->>A: 「类似刚才那部」
    A->>Ctx: resolveReference
    Ctx-->>A: 「重生之逆袭人生 类似」
    Note over A: ⚠️ 关键：不还原的话<br/>检索层不认识「刚才那部」<br/>必然空结果
    A->>M: recommend_drama(keyword=重生之逆袭人生)
    M-->>A: 同题材「都市励志」的剧
    A->>Ctx: recordDrama(更新为本轮剧名)
    A-->>U: 相似推荐结果
```

---

## 四、Spring AI function calling 时序（模型自主选工具）

```mermaid
sequenceDiagram
    autonumber
    participant Svc as SpringAiAgentService
    participant AI as 大模型
    participant Fn as Function Beans
    participant Mcp as MCP 服务

    Svc->>AI: Prompt(系统提示 + 历史 + 问题)<br/>+ tools=[queryDrama, recommendDrama,<br/>getHotRanking, getDramaDetail]

    alt 模型判断需要查数据
        AI-->>Svc: tool_calls: [{name:"recommendDrama",<br/>arguments:{keyword:"重生"}}]
        Svc->>Fn: 执行 recommendDrama
        Fn->>Mcp: POST /api/mcp/call
        Mcp-->>Fn: 推荐结果
        Fn-->>Svc: FunctionResponse(JSON)
        Svc->>AI: 把工具结果回传给模型
        AI-->>Svc: 整理后的自然语言推荐语
    else 模型判断无需工具
        AI-->>Svc: 直接回复（如闲聊、平台规则问答）
    end

    Svc-->>Svc: 返回最终文本
```

**关键**：工具描述（`withDescription`）决定模型能否选对工具，这是"SKILL"的本质——不需要训练专用模型。

---

## 五、AI 网关版时序（第二套编排，偏知识问答）

```mermaid
sequenceDiagram
    autonumber
    participant U as 用户
    participant G as Gateway
    participant AG as bunny-flix-aigateway (8804)
    participant IR as IntentRecognizer
    participant QR as QueryRouter
    participant R as RAG (8803)
    participant Gr as Graph (8805)
    participant L as LlmService

    U->>G: POST /api/aigateway/chat
    G->>AG: 转发
    AG->>IR: recognize(message)
    IR-->>AG: intent(RAG/GRAPH/HYBRID/LLM) + 置信度

    alt intent = RAG
        AG->>QR: routeToRag(query)
        QR->>R: 语义检索
        R-->>QR: 知识片段
        QR-->>AG: references
    else intent = GRAPH
        AG->>QR: routeToGraph(query)
        QR->>Gr: 图谱查询
        Gr-->>QR: 三元组关系
        QR-->>AG: graph
    else intent = HYBRID
        AG->>QR: 同时查 RAG + Graph
        QR-->>AG: references + graph
    else intent = LLM
        Note over AG: 无需检索，直接生成
    end

    AG->>L: answer(question, history, references, graph)
    L-->>AG: 回复
    AG-->>G: {reply, intent, route, references, graph}
    G-->>U: 响应
```

**两套编排的定位差异**：
- `bunny-flix-agent`：偏**工具调用型**对话（查剧、推荐、榜单）
- `bunny-flix-aigateway`：偏**知识问答型**对话（意图 → RAG/图谱）

---

## 六、降级链路总览

```mermaid
flowchart TD
    A[用户请求] --> B{Spring AI 可用?}
    B -->|是| C[模型 function calling 生成回复]
    B -->|否| D{原生 LLM 可用?}
    D -->|是| E[调用 OpenAI 兼容接口]
    D -->|否| F[本地模板兜底回复]

    C --> G{成功?}
    G -->|否| D
    E --> H{成功?}
    H -->|否| F

    F --> I[返回结果]
    C --> I
    E --> I

    J[RAG 检索] -->|失败| K[返回空列表, 不阻断]
    L[MCP 工具] -->|失败| M[记录 error, 不阻断]
    N[Redis 统计] -->|失败| O[降级, 不影响发号]
```

**设计原则**：旁路依赖故障不阻断主流程。详见 `消息中间件旁路降级隐患.md`。

---

## 七、服务端口对照

| 服务 | 端口 | 时序图中的角色 |
|---|---|---|
| `page-bunny-agent` | 5176 | 前端 |
| `bunny-flix-gateway` | 8080 | 网关（白名单放行 /api/agent） |
| `bunny-flix-agent` | 8801 | 对话编排主服务 |
| `bunny-flix-mcp` | 8802 | 工具服务（6 个工具） |
| `bunny-flix-rag` | 8803 | 语义检索 |
| `bunny-flix-aigateway` | 8804 | 第二套编排 |
| `bunny-flix-graph` | 8805 | 知识图谱 |

---

*文档生成时间：2026-09-12*
*配套：`AI对话服务拆解.md`、`面试.md`*
