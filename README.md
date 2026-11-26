# PrimaryPath

> 面向基层的单病种全流程助手 —— 基层医生在线上问诊中随叫随到的「数字同事」。

[![License](https://img.shields.io/badge/License-Apache--2.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](build.gradle.kts)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-6DB33F.svg)](build.gradle.kts)
[![LangChain4j](https://img.shields.io/badge/LangChain4j-1.18.0--beta28-7C3AED.svg)](build.gradle.kts)
[![Status](https://img.shields.io/badge/status-早期开发%20·%20M0-orange.svg)](PLAN.md)

- 不做全科 AI。只做单病种，但做全流程：问诊前的信息准备、问诊中的评估与处置线索、问诊后的随访、家医签约、健康档案和医保合规提示。
- 不做线上问诊平台。医院有自己的平台或用第三方的，需要这个能力就调接口接入。后端是核心，接口要少、要粗、容易对接。以后会写一个演示用的问诊平台，只为演示和验证框架，它自己也只是这套接口的一个调用方。
- 数据来源要可依。指南、共识、规范等入库做索引，给医生的建议每条都带出处（文档、版本、章节、页码），检索不到依据就明说，不编。
- 只辅助不决策。不自动下诊断，不自动生成处方，采纳与否由医生判断。

工作方式：

```text
医生端 ⇄ 患者端（医院自建或第三方问诊平台，或演示前端）
   │  患者基本信息 + 对话轮次
   ▼
PrimaryPath 后端
   接入层   租户鉴权 · 限流 · SSE 输出
   编排层   任务路由 → 检索 → 规则引擎 → 生成 → 引用校验
   知识层   病种包 + 知识库（解析、切片、索引、出处）
   模型层   LLM + Embedding，OpenAI 兼容协议
   │  带出处的建议：待问问题 / 评估线索 / 红旗征 / 随访与协作动作草稿
   ▼
医生审阅，自主决策
```

医生和患者在哪个平台聊不重要。接入方后端把患者基本信息和对话轮次发过来，系统按问诊进展返回带出处的建议，接入方前端渲染成医生侧栏。

当前处于早期开发（M0）：模型链路、数据库和骨架接口已通，其余见 [PLAN.md](PLAN.md)。

## 技术栈

| 层 | 选型 |
|---|---|
| 运行时 | Java 21（Gradle 工具链固定） |
| 框架 | Spring Boot 4.1.0（Web MVC、Data JPA） |
| 构建 | Gradle 9.5.1，Kotlin DSL + Wrapper（发行版走国内镜像，无需本地装 Gradle） |
| AI 编排 | LangChain4j 1.18.0-beta28（Spring Boot 4 Starter、OpenAI 模块、RAG、流式） |
| 大模型 | 任意 OpenAI 兼容接口，用 base-url / model-name 切换；开发环境用火山方舟豆包系列 |
| Embedding | all-MiniLM-L6-v2（进程内 ONNX），规划换中文模型 |
| 向量存储 | 内存 EmbeddingStore，规划迁 PostgreSQL + pgvector |
| 数据库 | PostgreSQL，表结构由 schema.sql 维护 |
| 认证 | 对外接入：API Key（`X-Api-Key`，带 scope，只存哈希）；用户登录：JWT（jjwt 0.12.x）+ BCrypt |
| 测试 | JUnit 5 + Spring Boot Test；另有 Lombok |

## 项目结构

```text
.
├── build.gradle.kts / settings.gradle.kts   # rootProject.name = backend（遗留命名）
├── gradlew / gradlew.bat / gradle/wrapper/  # Gradle Wrapper 9.5.1
├── README.md / PLAN.md / AGENTS.md / LICENSE
├── docs/auth-design.md                  # 鉴权与租户接入方案
└── src
    ├── main
    │   ├── java/com/example/backend
    │   │   ├── BackendApplication.java     # 启动类
    │   │   ├── ai/Assistant.java           # LangChain4j AiService：同步 + 流式对话
    │   │   ├── config/                     # RagConfig、WebMvcConfig、LlmConfigLogger、AuthProperties
    │   │   ├── controller/                 # AssistantController（SSE）、AuthController、AdminTenantController
    │   │   ├── service/                    # ChatService、AuthService、TenantService、ApiKeyService
    │   │   ├── security/                   # API Key 拦截器与 scope、JWT 签发与拦截器、ADMIN 校验
    │   │   ├── model/ repository/          # 用户、租户、接入凭证实体与仓库
    │   │   ├── dto/                        # 请求 / 响应（record）
    │   │   └── exception/                  # BusinessException、全局异常处理
    │   └── resources
    │       ├── application.yml             # 模型、数据源、JWT、LangChain4j 配置
    │       └── schema.sql                  # 幂等建表脚本
    └── test/java/com/example/backend       # 上下文加载测试
```

## 快速开始

环境：JDK 21（`java -version` 确认，不是 21 就设 JAVA_HOME）、PostgreSQL 14+、一个 OpenAI 兼容模型服务和 API Key。首次构建需联网拉依赖。

1. 克隆仓库：

```bash
git clone https://github.com/AB-programming/PrimaryPath.git
cd PrimaryPath
```

2. 建库，表在首次启动时自动创建（schema.sql 幂等，可重复执行）：

```sql
CREATE DATABASE agent;
```

3. 配置模型。仓库里的 application.yml 从环境变量读：

| 变量 | 必填 | 说明 |
|---|---|---|
| `AI_API_KEY` | 是 | 模型服务的 API Key |
| `AI_BASE_URL` | 是 | OpenAI 兼容接口地址 |
| `AI_MODEL` | 是 | 模型名 |
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | 否 | 覆盖默认的本机 5432 / 库 agent / 用户 postgres |

```powershell
# Windows PowerShell
$env:AI_API_KEY = "<你的 API Key>"
$env:AI_BASE_URL = "https://ark.cn-beijing.volces.com/api/v3"
$env:AI_MODEL = "<模型名>"
$env:SPRING_DATASOURCE_PASSWORD = "<数据库密码>"
```

```bash
# Linux / macOS
export AI_API_KEY="<你的 API Key>"
export AI_BASE_URL="https://ark.cn-beijing.volces.com/api/v3"
export AI_MODEL="<模型名>"
export SPRING_DATASOURCE_PASSWORD="<数据库密码>"
```

本地也可以直接改 application.yml，但密钥别提交进仓库。

4. 启动，默认端口 8080：

```powershell
# Windows
$env:JAVA_HOME = "<JDK 21 目录>"   # PATH 上的 java 不是 21 时才需要
.\gradlew.bat bootRun
```

```bash
# Linux / macOS
export JAVA_HOME=<JDK 21 目录>
./gradlew bootRun
```

5. 验证：

```bash
# 流式
curl -N -G "http://localhost:8080/assistant/chat/stream" --data-urlencode "message=你好" --data-urlencode "sessionId=demo"
# 非流式
curl -G "http://localhost:8080/assistant/chat" --data-urlencode "message=你好" --data-urlencode "sessionId=demo"
```

这两个是骨架接口，用来确认模型链路通了；正式接口见 PLAN.md。

管理端接口（`/admin/tenants`，建租户、发 key）需要 ADMIN 账号。先用 `/auth/register` 注册，再到库里把这个用户提为管理员，之后用 `/auth/login` 拿 token：

```sql
UPDATE users SET role = 'ADMIN' WHERE username = '<用户名>';
```

## 构建与部署

```bash
./gradlew bootJar        # Windows 用 .\gradlew.bat bootJar
java -jar build/libs/backend-0.0.1-SNAPSHOT.jar
```

fat jar 约 250 MB（含 PostgreSQL 驱动和进程内 embedding 的 ONNX 运行时）。配置方式同本地，也可外置配置文件：

```bash
java -jar build/libs/backend-0.0.1-SNAPSHOT.jar --spring.config.additional-location=file:/etc/primarypath/
```

Dockerfile 和 docker-compose 在 M3 里程碑。反向代理要注意 SSE 必须关缓冲，否则流式输出会被攒包：

```nginx
location / {
    proxy_pass http://127.0.0.1:8080;
    proxy_http_version 1.1;
    proxy_set_header Connection "";
    proxy_buffering off;
    proxy_cache off;
    proxy_read_timeout 300s;      # 大于单次生成最长耗时
}
```

## 对外接口（规划）

接入是服务端对服务端：租户凭证放在接入方后端，不下发到浏览器。流程是创建租户发凭证，接入方在问诊中调 `POST /v1/assist/stream`，前端把事件流渲染成医生侧栏。

v1 只开 7 个接口：

| 方法 | 路径 | 用途 |
|---|---|---|
| POST | /v1/assist/stream | 问诊辅助，SSE，核心入口 |
| POST | /v1/assist/feedback | 回传医生对建议的采纳 / 修改 |
| POST | /v1/knowledge/documents | 上传知识文档 |
| GET | /v1/knowledge/documents | 文档列表与处理状态 |
| DELETE | /v1/knowledge/documents/{id} | 删除文档及其索引 |
| GET | /v1/diseases | 可用病种、版本、任务类型 |
| GET | /v1/health | 健康检查 |

请求结构、SSE 事件协议、错误码和管理端接口见 [PLAN.md](PLAN.md)，鉴权方案见 [docs/auth-design.md](docs/auth-design.md)。

## 当前进度

| 模块 | 状态 |
|---|---|
| 工程骨架、模型接入（同步 + 流式）、PG 建表、JWT 登录 | 已完成 |
| 租户接入鉴权：tenants / api_keys 表、API Key 拦截器与 scope、管理端接口、开放注册开关 | 已完成；`/v1/**` 还没有接口，拦截器已挂上。限流和知识库按租户隔离在 M3 |
| RAG 组件 | 骨架：内存向量库 + 本地 embedding，没有入库接口，重启清空 |
| 知识库管理、病种包、正式辅助接口、租户隔离与限流、全流程能力、评测审计、演示前端 | 规划中，见 [PLAN.md](PLAN.md) 的里程碑 |

## 许可证

Apache License 2.0。项目输出仅作辅助参考，不构成诊疗建议；诊断与处方由具备资质的医师本人作出。
