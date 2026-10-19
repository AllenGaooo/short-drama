# bunny-flix-server（短剧后台）

短剧（bunny-flix）业务的后端服务集合，基于 Spring Cloud 的微服务架构。

## 技术栈

- Java 17 / Maven
- Spring Cloud Alibaba 微服务
- 各业务服务（`service-*`）+ AI 能力模块（`bunny-flix-agent` / `rag` / `mcp` / `aigateway` / `graph`）
- 前端：`bunny-flix-web`、`page-bunny-mobile`、`page-bunny-agent`、`page-bunnuy-admin`

## 模块一览

| 类型 | 模块 |
|------|------|
| 公共 | `bunny-flix-common` |
| 网关 | `bunny-flix-gateway`、`bunny-flix-aigateway` |
| 业务服务 | `service-admin` `service-coin` `service-data` `service-drama` `service-id-generator` `service-interaction` `service-marketing` `service-member` `service-push` `service-recommend` `service-search` `service-user` `service-video` |
| AI | `bunny-flix-agent` `bunny-flix-rag` `bunny-flix-mcp` `bunny-flix-graph` |
| 前端 | `bunny-flix-web` `page-bunny-mobile` `page-bunny-agent` `page-bunnuy-admin` |
| 其它 | `sql/`（建表脚本）、`doc/`（文档） |

## 构建

```bash
mvn clean package -DskipTests
```

> ⚠️ 配置中的密钥（数据库、第三方 API 等）已从仓库中脱敏为占位符 `<REDACTED>`，请通过环境变量 / 配置中心在部署时注入真实值。
