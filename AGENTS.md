# AGENTS.md

给在本仓库工作的编程智能体。先读 [README.md](README.md)（项目是什么）和 [PLAN.md](PLAN.md)（要做成什么），本文管三件事：代码现状、怎么改、别碰什么。

§2 到 §5 是 2026-11-26 逐文件核对源码的结果，以源码为准；跟源码对不上就先指出，别按本文的旧描述行事。要改本文的规则性内容，先跟用户提。

## 边界

1. 后端是产品，接口要少。对外接口以 PLAN.md 的清单为准；加接口先说明现有接口为什么不够，并同步 PLAN.md。
2. 不做问诊平台：IM、挂号、支付、处方流转都不做，演示前端只能用公开接口。
3. 给医生的建议必须能对到知识库原文（文档、版本、章节、页码）；检索不到依据就明说，不编。
4. 只辅助：不自动诊断，不自动生成处方。
5. 病种逻辑放病种包里，新病种不改编排层。
6. 日志、异常、审计里不落患者身份信息和对话明文；密钥不进仓库、不进日志。
7. 删文件、改提交历史、清数据这类破坏性操作先问用户。

## 现状

构建与依赖：

| 项 | 值 |
|---|---|
| 构建 | Gradle 9.5.1，Kotlin DSL，Wrapper 已提交（发行版走腾讯云镜像） |
| JDK | Java 21（toolchain 固定） |
| 框架 | Spring Boot 4.1.0：webmvc、data-jpa、security-crypto、devtools |
| 认证 | jjwt 0.12.x（api / impl / jackson） |
| AI | LangChain4j 1.18.0-beta28：spring-boot4-starter、open-ai-spring-boot4-starter、embeddings-all-minilm-l6-v2 |
| 其他 | PostgreSQL 驱动、Lombok；测试用 spring-boot-starter-webmvc-test + JUnit 5 |

LangChain4j 是 beta、Spring Boot 4.1 较新：不熟的类先查对应版本文档，升级依赖后跑全量测试。

代码（包根 `com.example.backend`）：

| 位置 | 内容 |
|---|---|
| `ai/Assistant` | @AiService 接口：`chat()` 同步，`chatStream()` 返回 TokenStream；system prompt 还是通用助手文案 |
| `config/RagConfig` | AllMiniLmL6V2EmbeddingModel、InMemoryEmbeddingStore、Ingestor（`recursive(800, 100)` 切片）、ContentRetriever（取前 3 条，minScore 0.6） |
| `config/WebMvcConfig` | 注册 JwtAuthInterceptor，目前只拦 `/auth/me` |
| `config/LlmConfigLogger` | 启动时打印生效的 base-url 和模型名，不打印密钥 |
| `controller/AssistantController` | `/assistant/chat`、`/assistant/chat/stream`；TokenStream 绑 SseEmitter 在这层做 |
| `controller/AuthController` | `/auth/register`、`/auth/login`、`/auth/me` |
| `service/ChatService`、`AuthService` | ChatService 调 Assistant；AuthService 管注册登录，用户名 3–32 位字母数字下划线，密码至少 6 位 |
| `security/` | JwtTokenProvider：HS256，`sub` 用户名、`uid` 用户 id，默认 7 天；JwtAuthInterceptor 校验 Bearer token，把 userId / username 塞进 request attribute |
| `model/`、`repository/` | UserInfo（BCrypt 哈希，`@JsonIgnore`）、UserRepository |
| `dto/` | 三个 record：AuthResponse、LoginRequest、RegisterRequest |
| `exception/` | BusinessException（工厂方法 badRequest / notFound / conflict / unauthorized），GlobalExceptionHandler 统一转响应 |

配置（application.yml）：

| 键 | 说明 |
|---|---|
| `spring.datasource.*` | PG 连接，默认本机 5432、库名 agent、用户 postgres |
| `spring.jpa.hibernate.ddl-auto` | none，表结构不走 Hibernate |
| `spring.sql.init` | always + classpath:schema.sql，每次启动执行建表脚本 |
| `ai.llm.*` | 自定义键，被下方 starter 引用；提交版本从 `AI_API_KEY` / `AI_BASE_URL` / `AI_MODEL` 环境变量读 |
| `langchain4j.open-ai.chat-model / streaming-chat-model` | 同步和流式模型；log-requests / log-responses 是开着的，会记完整请求响应 |
| `jwt.secret` / `jwt.expiration-ms` | HS256 密钥（至少 32 字节）/ 有效期 |

数据表（schema.sql）：

| 表 | 使用情况 |
|---|---|
| `users` | 已用（UserInfo） |
| `sessions`、`chat_memory` | 没用，无代码引用 |
| `documents` | 没用，无实体、无上传接口 |

HTTP 接口（业务错误统一返回 `{"message": "..."}`）：

| 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|
| POST | /auth/register、/auth/login | 无 | 返回 token 和用户信息 |
| GET | /auth/me | JWT | 当前用户 |
| GET | /assistant/chat | 无 | 同步对话，参数 message、sessionId |
| GET | /assistant/chat/stream | 无 | SSE 流式，参数同上 |

已实现：模型链路（同步 + 流式）、PG、JWT 登录、RAG 组件的 Bean。
未实现：文档入库、向量持久化、病种包与规则引擎、/v1/assist/*、租户鉴权、会话记忆、审计评测、健康检查、Dockerfile、CI。

## 编码约定

沿用现有代码风格：

- controller 只收参数、调 service、做协议转换（TokenStream 到 SseEmitter 的绑定在这层）；业务逻辑在 service；请求 / 响应对象在 dto。
- 构造器注入：`@RequiredArgsConstructor` 加 final 字段，不用字段上的 `@Autowired`。
- DTO 用 record；实体用 `@Data @Builder @NoArgsConstructor @AllArgsConstructor`，敏感字段 `@JsonIgnore`。
- 业务错误在 service 抛 BusinessException（用工厂方法），Controller 不自己拼错误响应。
- 日志 `@Slf4j`，带模块前缀（[Auth] [Chat] [Stream] [Tool] [LLM] [Business]），用 `{}` 占位符。
- 改表只改 schema.sql，语句必须幂等，因为每次启动都执行。
- 版本优先交给 Boot BOM 管；LangChain4j 各模块版本保持一致；加依赖要说明用途。
- 注释、日志、错误提示用中文，标识符用英文。
- 提交信息 `type: 简述`（feat / fix / refactor / docs / test / chore）。

## 命令

```powershell
java -version                                   # 必须 21，否则先设 JAVA_HOME
.\gradlew.bat bootRun                           # 启动（Windows）
.\gradlew.bat test                              # 全量测试（需要 PG）
.\gradlew.bat test --tests "com.example.backend.BackendApplicationTests"
.\gradlew.bat bootJar                           # 打包 → build/libs/backend-0.0.1-SNAPSHOT.jar
```

Linux / macOS 把 `.\gradlew.bat` 换成 `./gradlew`。

运行前提：PG 里建好 `agent` 库；`AI_API_KEY`、`AI_BASE_URL`、`AI_MODEL` 配好。IDE 里的 Gradle 和 JVM 可能跟 wrapper 不一致，以命令行为准。

交付前：没实际跑过的校验不得说"已通过"；改了对外接口就同步 PLAN.md 和 README.md；测试不为凑绿而删改。

## 已知问题

1. Assistant 的 system prompt 提到计算器、天气、日期工具，项目里没注册任何工具。
2. 没有多轮记忆：sessionId 只进日志，chat_memory 表没人用。
3. 知识库是空的：documents 表无实体无接口，Ingestor 没有调用方，InMemoryEmbeddingStore 重启清空。
4. all-MiniLM-L6-v2 偏英文，中文医学文本的检索要重新选型。
5. `/assistant/**` 无鉴权；现有 JWT 是用户登录体系，不是租户接入鉴权。
6. log-requests / log-responses、消息日志、工具结果日志会带患者数据，接真实数据前必须默认关或脱敏。
7. 提交版本里 jwt.secret 和数据库密码是明文字面量（开发默认值），上线前换环境变量。
8. 命名遗留：rootProject.name=backend、spring.application.name=agent、包名 com.example.backend、库名 agent，和 PrimaryPath 对不上；重命名涉及面广，要用户确认后单独做。
9. GlobalExceptionHandler 只处理 BusinessException 和 DuplicateKeyException，参数校验失败、404 走 Spring 默认响应，格式不统一。
10. 没有健康检查端点、Dockerfile、CI。
