# 鉴权与租户接入方案（设计稿）

状态：2026-11-26 确认并实施（scope 4 个、管理端接口进 PLAN.md、key 前缀 pp_）。第 7 节是最小闭环的落地情况；限流、key 缓存、租户隔离留 M3。

## 结论

- 现有 JWT 登录是用户（人）登录，不是对外接入鉴权，两者不互相替代。给演示前端和管理端登录用，够用；给三方平台调接口用，不够用。
- 对外接口用 API Key（`X-Api-Key` 头），key 上挂 scope（能力清单），不引入 OAuth2。
- 权限角色：对外用 scope，对内加一个 ADMIN 角色，完整 RBAC 不做。医生个人不进鉴权体系。

## 1. 现状评估

| 问题 | 现状 | 作为演示前端登录 | 作为对外接入 |
|---|---|---|---|
| 主体 | users 表，人 | 够用 | 不够：三方平台是机构，不是用户 |
| 凭证 | JWT 7 天，HS256 | 够用 | 不够：泄露后无法吊销单张 token，只能换全局密钥让所有用户下线 |
| 权限粒度 | 无 | 够用（只有 /auth/me） | 不够：无法区分只读联调、正式调用、文档上传 |
| 接口覆盖 | 只拦 /auth/me | 够用 | 不够：/assistant/** 公开 |
| 注册 | 开放 | 可接受 | 不可接受：管理端若复用这套登录，等于人人可注册 |
| 密钥 | jwt.secret、数据库密码明文在仓库 | 开发期可接受 | 发放真实凭证前必须外置（已知问题 7） |

## 2. 总体结构

两条独立的认证链，互不复用：

| 链 | 路径 | 凭证 | 主体 | 拦截器 |
|---|---|---|---|---|
| 租户接入 | /v1/** | X-Api-Key | 租户（机构） | ApiKeyAuthInterceptor（新增） |
| 用户登录 | /auth/**、/admin/** | Bearer JWT | 人 | JwtAuthInterceptor（现有，扩路径）+ AdminRequiredInterceptor（查库验 ADMIN） |

分两条的原因：调用方性质不同（机器对机器 vs 人）；凭证生命周期不同（长期可吊销的 key vs 短期 token）；对接成本不同（第三方半天联调通是 PLAN 的验收线，API Key 是最低成本方式）。两条互不牵连，一条出问题不影响另一条。

不用 OAuth2 / mTLS / 请求签名的原因：

- OAuth2 client credentials：多一个 token 端点、过期刷新和凭证存储逻辑，换来的能力（授权委派、多 provider）用不上，只有一个 provider 的服务间调用不值。
- mTLS：对接运维成本高，合作方能力参差，TLS + key 已覆盖传输安全。
- HMAC 请求签名：解决防篡改和防重放，HTTPS 下 v1 不需要；将来有合规要求再加，加的时候 key 密钥可以复用，不用推翻本方案。

/v1 不做浏览器直连：租户凭证放接入方后端，不下发浏览器（README 已定），所以不需要 CORS。

## 3. 租户与凭证

表结构（进 schema.sql，幂等）：

```sql
CREATE TABLE IF NOT EXISTS tenants (
    id         VARCHAR(32)  PRIMARY KEY,
    name       VARCHAR(200) NOT NULL,
    status     VARCHAR(16)  NOT NULL DEFAULT 'active',   -- active / suspended
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS api_keys (
    id           VARCHAR(16)  PRIMARY KEY,                -- 即 key 前缀里的 kid
    tenant_id    VARCHAR(32)  NOT NULL,
    name         VARCHAR(200) NOT NULL,                   -- 用途备注，如「XX平台生产-1」
    secret_hash  CHAR(64)     NOT NULL,                   -- SHA-256(secret) 十六进制
    scopes       VARCHAR(200) NOT NULL,                   -- 逗号分隔
    status       VARCHAR(16)  NOT NULL DEFAULT 'active',  -- active / revoked
    expires_at   TIMESTAMP,
    last_used_at TIMESTAMP,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at    TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_api_keys_tenant ON api_keys (tenant_id);
```

租户的启用病种列表、限流参数、知识库隔离字段 M3 再加表（tenant_diseases 等），本方案只定租户主键和凭证关系。

key 的形态与发放：

- 格式 `pp_<kid>_<secret>`：pp 是系统前缀，方便密钥扫描器识别和人工排查；kid 10 位，secret 43 位（SecureRandom 32 字节 base64url），全长约 57 字符。
- 发放时完整 key 只显示一次，库里只存 SHA-256(secret)。校验按 kid 查行、比对哈希，用 MessageDigest.isEqual。
- 一个租户可持多把 key：轮换用，也按接入系统分离（问诊平台一把、院内系统一把）。
- 吊销即时生效：每次请求查库校验 status。当前单实例，一次索引查询可接受；多实例后加 30 秒级本地缓存。
- last_used_at 不逐请求写库：距上次落库超过 5 分钟才更新。只用于运营排查和轮换判断，不做计费依据。
- 请求日志记 tenant_id、key id、request_id，不记 key 明文（边界 6）。

轮换流程，全程不停服：

1. 管理端给租户发第二把 key（同 scope）。
2. 通知接入方换配置。
3. 观察旧 key last_used_at 不再前进。
4. 吊销旧 key。

## 4. 对外权限：scope

key 上的能力清单，平铺无层级：

| scope | 允许 | 对应接口 |
|---|---|---|
| assist | 问诊辅助 | POST /v1/assist/stream |
| feedback | 回传反馈 | POST /v1/assist/feedback |
| knowledge:read | 文档列表与状态 | GET /v1/knowledge/documents |
| knowledge:write | 上传删除文档 | POST、DELETE /v1/knowledge/documents |
| （无需 scope） | 能力发现、健康检查 | GET /v1/diseases、/v1/health |

默认组合：问诊平台型租户发 assist + feedback；医院型租户（要传院内规范）加 knowledge:read + knowledge:write；联调期可先发只读组合。

后面扩接口：新增 `/v1` 路径时在 `ApiScope.ROUTES` 登记一行（方法、路径模式、所需 scope；要新 scope 就在枚举里加值）。没登记的路径默认 403，新接口不会因为忘了配而裸奔。

错误语义：key 缺失、无效、吊销、过期统一 401 UNAUTHORIZED；scope 不足 403，错误码建议 FORBIDDEN（同步 PLAN.md 时补进错误码表）；租户停用 403；病种未启用 403 DISEASE_NOT_ENABLED（PLAN 已有）。/v1 链路的错误响应体带 code 字段，与 PLAN 错误码表对齐；现有 /auth 链路的 `{"message": ...}` 格式不动。

## 5. 对内权限：role

users 加一列 role，取值 USER / ADMIN：

- USER：演示前端登录，行为与现状一致。
- ADMIN：管理端（租户、凭证、文档审核）。
- JWT 只装身份，sub/uid 不变，不放 role。/admin/** 在 JWT 校验通过后查一次库取 role：admin 流量小，一次查询换角色变更即时生效，不用等 7 天 token 过期。
- 开放注册加开关 `auth.open-registration`，默认 true 维持现状；对外部署改 false，账号由 ADMIN 创建。目前没有建账号的管理接口，第一个 ADMIN 只能用 SQL 改 role（README 快速开始有命令）。

不上完整 RBAC、不引 Spring Security 的原因：角色就两个，权限点没多到需要 RBAC；先加一列，管理端功能长出来（文档审核、租户管理分人负责）再评估。现有拦截器 + jjwt 的写法链路直白、代码量小；spring-boot-starter-security 带来 filter chain 和配置面的扩大，当前收益不匹配。将来要做密码重置、会话管理、CSRF 这类成体系功能时再迁移。

管理端接口（已实现，清单见 PLAN.md）：

| 方法 | 路径 | 用途 |
|---|---|---|
| POST | /admin/tenants | 建租户 |
| GET | /admin/tenants | 租户列表（含 key 元数据，不含密钥） |
| PATCH | /admin/tenants/{id} | 停用 / 启用租户 |
| POST | /admin/tenants/{id}/keys | 发 key（响应里唯一一次返回明文） |
| DELETE | /admin/tenants/{id}/keys/{keyId} | 吊销 |

现有接口不够的原因：目前没有任何租户管理入口，而这是对外接入的前置条件；属 PLAN M3 已列的租户与凭证管理，不是新增产品能力。

## 6. 医生的角色

医生不是本系统的鉴权主体，身份在接入方平台（患者身份不进系统，PLAN 已定）。要按医生归因（谁采纳了建议）时，接入方在 feedback 请求里带 opaque 的 operator_id，进审计日志，仅此而已。不建医生账号，不做医生角色映射。患者侧能力 v1 不做（PLAN 待决策已列）。

## 7. 落地情况

已实施（2026-11-26）：

1. schema.sql：tenants、api_keys 幂等建表；users 建表语句加 role 列，另加 `ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(16) NOT NULL DEFAULT 'USER'` 兼容已有库。
2. ApiKeyAuthInterceptor，注册到 /v1/**（排除 /v1/health），401 / 403 响应体为 `{"code","message"}`。
3. scope 校验：ApiScope.ROUTES 一张表，没登记的路径默认 403。
4. 管理端 5 个接口，JWT + ADMIN；`auth.open-registration` 开关。
5. 测试：编解码、scope 路由、拦截器单测，加一个走 PG 的管理端接口测试。`./gradlew test` 共 20 个用例。

实现时发现并修掉的一个坑：secret 是 base64url，字母表含 `_`，明文不能整串按 `_` 切，只能按前两个切，否则约四分之一的 key 解析失败。

留到 M3：限流、key 的多实例缓存、知识库按租户隔离。

发真实凭证前必须做（与已知问题 7 一起）：jwt.secret、数据库密码改环境变量；对外部署强制 HTTPS、关掉开放注册；/assistant 骨架接口加开发开关，或随 /v1/assist/stream 上线下线。

## 8. 已定

| 事项 | 结论 |
|---|---|
| scope 集合 | 4 个；扩接口时在 ApiScope.ROUTES 登记 |
| 管理端接口 | 进 PLAN.md，已实现 |
| key 前缀 | pp_，不再改 |
| 限流参数 | M3 定（按租户 QPS + 并发 SSE 数） |
| 开放注册开关默认值 | 保持 true；对外部署改 false |
