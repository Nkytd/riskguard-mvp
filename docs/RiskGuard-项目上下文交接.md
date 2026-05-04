# RiskGuard 项目上下文交接

版本：v0.1  
日期：2026-05-01  
分支：`codex/riskguard-mvp`  
最新提交：`baf6fec feat: implement RiskGuard backend MVP`  
远程仓库：`https://github.com/Nkytd/riskguard-mvp.git`

## 1. 项目定位

RiskGuard 是一个面向登录、支付等关键业务场景的实时风控决策平台 MVP。

当前目标不是做完整金融级反欺诈算法平台，而是做一个能体现 Java 后端工程能力的可运行系统：

- 可配置规则
- 可发布策略
- 实时风险决策
- 决策日志追踪
- REVIEW 风险案件审核闭环
- Docker 本地依赖环境
- 可重复 smoke test

项目主线：

```text
业务事件 -> 加载策略快照 -> 构建风险上下文 -> 执行规则表达式 -> 计算风险分 -> 返回决策 -> 落日志 -> REVIEW 建案
```

## 2. 关键技术决策

### 2.1 架构决策

- 采用模块化单体架构，而不是一开始拆微服务。
- 包结构按未来可拆服务边界组织。
- 实时决策主链路保持同步、清晰、可追溯。
- RabbitMQ 暂时作为依赖和后续扩展预留，当前日志和案件阶段采用同步落库。
- AI、Elasticsearch、Flink 暂缓，避免 MVP 范围失控。

### 2.2 技术栈

- Java：`Java 17+`，本机实际使用 Java 21 运行验证。
- 后端：`Spring Boot 3.3.5`
- ORM：`MyBatis-Plus 3.5.9`
- 数据库：`MySQL 8.4`
- 缓存：`Redis 7.4`
- MQ：`RabbitMQ 3.13-management`
- 规则表达式：`Aviator`
- API 文档：`Knife4j / OpenAPI`
- 构建：`Maven`
- 容器：`Docker Compose`

### 2.3 规则与策略决策

- 规则是最小判断单元，策略是一组规则的组合。
- 规则发布后生成 `risk_rule_version`。
- 策略绑定的是规则版本，而不是规则本身。
- 策略发布后生成 `risk_strategy_version`，其中包含规则快照 JSON。
- 决策引擎执行的是已发布策略快照，避免规则编辑污染历史决策。
- 表达式上下文使用嵌套结构，例如：

```text
event.amount >= 1000 && user.registerDays <= 7
device.accountCount24h >= 5
list.ipBlacklistHit == true
```

### 2.4 决策与幂等决策

- 风险分限制在 `0-100`。
- 规则动作优先级：

```text
REJECT > REVIEW > VERIFY > SCORE
```

- 未命中强动作规则时，按分数区间解析决策：

```text
0-30   -> LOW      -> PASS
31-60  -> MEDIUM   -> VERIFY
61-80  -> HIGH     -> REVIEW
81-100 -> CRITICAL -> REJECT
```

- 正式决策接口 `POST /api/v1/risk/decisions` 支持幂等：
  - 优先从 Redis `risk:idempotent:{requestNo}` 读取。
  - Redis 不可用时，按 `request_no` 从 `risk_decision_log` 恢复历史结果。
  - 数据库 `risk_event.request_no` 唯一索引作为兜底。

### 2.5 Smoke Test 决策

所有关键阶段完成后必须通过 smoke test。

当前 smoke test 覆盖：

- 健康检查
- 规则分页
- 策略分页
- 用户画像查询
- 实时决策
- 幂等二次调用
- 决策日志查询
- 命中规则日志查询
- REVIEW 自动建案
- 案件领取
- 案件审核通过
- 案件操作记录

## 3. 已完成阶段

### 3.1 阶段 0：项目骨架

已完成：

- Maven/Spring Boot 项目骨架
- `application.yml`
- Dockerfile
- Docker Compose
- MySQL 初始化 SQL
- 统一响应 `ApiResponse`
- 全局异常处理
- TraceId Filter
- Spring Security 基础配置
- MyBatis-Plus 分页插件
- OpenAPI 配置
- README

重要文件：

- `pom.xml`
- `src/main/java/com/riskguard/RiskGuardApplication.java`
- `src/main/resources/application.yml`
- `Dockerfile`
- `docker-compose.yml`
- `docker/mysql/init/001_schema.sql`
- `docker/mysql/init/002_seed.sql`

### 3.2 阶段 1：基础 CRUD

已完成：

- 规则管理 `risk_rule`
- 策略管理 `risk_strategy`
- 黑白名单管理 `risk_list`
- 用户画像查询
- 设备画像查询
- IP 画像查询
- 分页查询、启用、停用、删除等基础能力

主要接口：

```text
POST   /api/v1/rules
PUT    /api/v1/rules/{id}
GET    /api/v1/rules
GET    /api/v1/rules/{id}
POST   /api/v1/rules/{id}/enable
POST   /api/v1/rules/{id}/disable

POST   /api/v1/strategies
PUT    /api/v1/strategies/{id}
GET    /api/v1/strategies
GET    /api/v1/strategies/{id}
POST   /api/v1/strategies/{id}/enable
POST   /api/v1/strategies/{id}/disable

POST   /api/v1/risk-lists
PUT    /api/v1/risk-lists/{id}
GET    /api/v1/risk-lists
GET    /api/v1/risk-lists/{id}
POST   /api/v1/risk-lists/{id}/enable
POST   /api/v1/risk-lists/{id}/disable
DELETE /api/v1/risk-lists/{id}

GET    /api/v1/profiles/users/{userId}
GET    /api/v1/profiles/devices/{deviceId}
GET    /api/v1/profiles/ips/{ip}
```

### 3.3 阶段 2：规则与策略发布

已完成：

- 规则发布生成版本。
- 规则版本查询。
- Aviator 表达式校验。
- 策略绑定规则版本。
- 策略规则排序。
- 策略发布生成规则快照。
- 策略版本查询。
- 策略回滚。
- Redis 策略缓存刷新预留。

主要接口：

```text
POST /api/v1/rules/{id}/publish
GET  /api/v1/rules/{id}/versions
POST /api/v1/rules/validate-expression

POST   /api/v1/strategies/{id}/rules
GET    /api/v1/strategies/{id}/rules
DELETE /api/v1/strategies/{id}/rules/{ruleId}
PUT    /api/v1/strategies/{id}/rules/order
POST   /api/v1/strategies/{id}/publish
POST   /api/v1/strategies/{id}/rollback
GET    /api/v1/strategies/{id}/versions
```

### 3.4 阶段 3：实时决策主链路

已完成：

- 正式决策接口。
- 模拟决策接口。
- 加载已发布策略快照。
- 构建规则上下文。
- 执行 Aviator 表达式。
- 计算风险分。
- 解析风险等级和决策结果。
- Redis 幂等缓存。
- DB 幂等兜底预留在阶段 4 完成。

主要接口：

```text
POST /api/v1/risk/decisions
POST /api/v1/risk/decisions/simulate
```

核心文件：

- `src/main/java/com/riskguard/decision/service/RiskDecisionService.java`
- `src/main/java/com/riskguard/engine/DecisionEngine.java`
- `src/main/java/com/riskguard/engine/context/ActiveStrategyLoader.java`
- `src/main/java/com/riskguard/engine/context/RiskContextBuilder.java`
- `src/main/java/com/riskguard/engine/evaluator/RuleEvaluator.java`
- `src/main/java/com/riskguard/engine/resolver/DecisionResolver.java`

### 3.5 阶段 4：日志与案件闭环

已完成：

- 风控事件落库 `risk_event`
- 决策日志落库 `risk_decision_log`
- 命中规则日志 `risk_decision_hit_rule`
- 决策日志分页查询
- 决策详情查询
- 命中规则查询
- `REVIEW` 自动生成风险案件
- 案件分页查询
- 案件详情查询
- 案件领取
- 案件审核通过
- 案件审核拒绝
- 案件关闭
- 标记误报
- 案件操作日志查询

主要接口：

```text
GET /api/v1/risk/decisions
GET /api/v1/risk/decisions/{decisionNo}
GET /api/v1/risk/decisions/{decisionNo}/hit-rules

GET  /api/v1/cases
GET  /api/v1/cases/{caseNo}
POST /api/v1/cases/{caseNo}/claim
POST /api/v1/cases/{caseNo}/approve
POST /api/v1/cases/{caseNo}/reject
POST /api/v1/cases/{caseNo}/close
POST /api/v1/cases/{caseNo}/mark-false-positive
GET  /api/v1/cases/{caseNo}/operations
```

核心文件：

- `src/main/java/com/riskguard/decision/service/DecisionPersistenceService.java`
- `src/main/java/com/riskguard/riskcase/service/RiskCaseService.java`
- `src/main/java/com/riskguard/riskcase/controller/RiskCaseController.java`
- `src/main/java/com/riskguard/decision/controller/RiskDecisionController.java`

### 3.6 阶段 5：Dashboard 看板统计

已完成：

- 看板总览统计
- 决策结果分布
- 规则命中 Top 10
- 最近 24 小时风险趋势
- 案件状态统计
- 审核通过率和拒绝率
- Dashboard 单元测试
- Smoke test 覆盖 Dashboard 接口

主要接口：

```text
GET /api/v1/dashboard/overview
GET /api/v1/dashboard/decision-distribution
GET /api/v1/dashboard/rule-hit-rank
GET /api/v1/dashboard/risk-trend
GET /api/v1/dashboard/case-statistics
```

核心文件：

- `src/main/java/com/riskguard/dashboard/controller/DashboardController.java`
- `src/main/java/com/riskguard/dashboard/service/DashboardService.java`
- `src/main/java/com/riskguard/dashboard/mapper/DashboardMapper.java`
- `src/test/java/com/riskguard/dashboard/DashboardServiceTests.java`

### 3.7 阶段 7：认证与权限

已完成：

- 登录接口
- 当前登录用户查询
- 基于 `sys_user` 的数据库用户认证
- JWT 签发和解析
- JWT 认证过滤器
- 统一 401/403 安全响应
- 后台管理接口角色保护
- 案件操作优先使用登录用户作为操作人
- Smoke test 覆盖登录和受保护接口访问

主要接口：

```text
POST /api/v1/auth/login
GET  /api/v1/auth/me
```

权限策略：

```text
GET  /api/v1/health                      -> 放行
POST /api/v1/auth/login                  -> 放行
POST /api/v1/risk/decisions              -> 放行，保留业务系统实时决策接入通道
POST /api/v1/risk/decisions/simulate     -> ADMIN / RISK_OPERATOR
规则、策略、名单管理接口                  -> ADMIN / RISK_OPERATOR
案件写操作                                -> ADMIN / RISK_OPERATOR
案件查询、决策日志、画像、Dashboard       -> ADMIN / RISK_OPERATOR / AUDITOR
```

核心文件：

- `src/main/java/com/riskguard/auth/controller/AuthController.java`
- `src/main/java/com/riskguard/auth/service/AuthService.java`
- `src/main/java/com/riskguard/auth/service/SysUserDetailsService.java`
- `src/main/java/com/riskguard/common/security/JwtService.java`
- `src/main/java/com/riskguard/common/security/JwtAuthenticationFilter.java`
- `src/main/java/com/riskguard/common/security/SecurityConfig.java`
- `src/test/java/com/riskguard/common/security/JwtServiceTests.java`

### 3.8 阶段 6：异步与画像更新

已完成：

- RabbitMQ exchange、queue、binding 配置
- 决策创建事件 `risk.decision.created`
- 案件创建事件 `risk.case.created`
- 事务提交后发布 MQ 事件
- MQ 发布失败降级为日志告警，不回滚已完成决策
- 决策事件消费者异步更新用户、设备、IP 画像统计
- Smoke test 覆盖异步画像创建

当前队列与路由：

```text
exchange: riskguard.events
queue:    riskguard.profile.update      <- risk.decision.created
queue:    riskguard.dashboard.aggregate <- risk.decision.created / risk.case.created
```

核心文件：

- `src/main/java/com/riskguard/mq/config/RabbitMqConfig.java`
- `src/main/java/com/riskguard/mq/publisher/RiskEventPublisher.java`
- `src/main/java/com/riskguard/mq/consumer/ProfileUpdateConsumer.java`
- `src/main/java/com/riskguard/profile/service/ProfileAsyncUpdateService.java`
- `src/test/java/com/riskguard/profile/ProfileAsyncUpdateServiceTests.java`

## 4. 当前验证状态

### 4.1 编译与测试

最近一次验证结果：

```text
mvn -q -DskipTests compile 通过
mvn -q test 通过
mvn -q -DskipTests package 通过
smoke test 通过
```

由于本机 `mvn` 不在 PATH，当前使用 IntelliJ 自带 Maven：

```powershell
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.3\plugins\maven\lib\maven3\bin\mvn.cmd' -q test
```

### 4.2 Smoke Test

Smoke test 脚本：

```text
scripts/smoke/run-smoke.ps1
```

推荐命令：

```powershell
$env:RISKGUARD_MYSQL_PORT='23306'
$env:RISKGUARD_REDIS_PORT='6380'
$env:RISKGUARD_RABBITMQ_PORT='5673'
$env:RISKGUARD_RABBITMQ_MANAGEMENT_PORT='15673'
docker compose -f .\docker-compose.yml up -d mysql redis rabbitmq

& 'C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.3\plugins\maven\lib\maven3\bin\mvn.cmd' -q -DskipTests package
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\smoke\run-smoke.ps1 -Port 28080 -MysqlPort 23306 -RedisPort 6380 -RabbitPort 5673
```

最近一次 smoke 输出：

```json
{
  "healthCode": 0,
  "authUser": "admin",
  "authRole": "ADMIN",
  "ruleCount": 8,
  "strategyCount": 2,
  "userProfile": "U10001",
  "decision": "REJECT",
  "riskScore": 100,
  "riskLevel": "CRITICAL",
  "hitRuleCount": 3,
  "idempotentSecondCall": true,
  "decisionLogFound": true,
  "hitRuleLogCount": 3,
  "reviewDecision": "REVIEW",
  "reviewCaseNo": "C20260501235146c8d8648b",
  "asyncProfileUser": "U-SMOKE-REVIEW-20260501235146",
  "asyncProfilePayCount24h": 1,
  "reviewCaseInitialStatus": "PENDING",
  "reviewCaseClaimedStatus": "PROCESSING",
  "reviewCaseFinalStatus": "APPROVED",
  "reviewCaseOperationCount": 3,
  "dashboardTodayDecisionCount": 14,
  "dashboardDistributionCount": 4,
  "dashboardRuleHitRankCount": 3,
  "dashboardTrendBucketCount": 24,
  "dashboardCaseTotalCount": 7
}
```

### 4.3 本地端口注意事项

本机已有 KnowFlow 容器占用部分默认端口，因此 RiskGuard smoke test 使用备用端口：

```text
MySQL:    23306 -> 3306
Redis:    6380  -> 6379
RabbitMQ: 5673  -> 5672
RabbitMQ Management: 15673 -> 15672
Backend smoke port: 28080
```

`docker-compose.yml` 已支持环境变量配置端口：

```text
RISKGUARD_MYSQL_PORT
RISKGUARD_REDIS_PORT
RISKGUARD_RABBITMQ_PORT
RISKGUARD_RABBITMQ_MANAGEMENT_PORT
```

## 5. 整体架构思路

### 5.1 包结构

```text
com.riskguard
├── common        # 通用响应、异常、枚举、缓存、TraceId、安全配置
├── auth          # 登录、数据库用户认证、JWT 签发
├── rule          # 规则 CRUD、发布、版本
├── strategy      # 策略 CRUD、规则绑定、发布、快照、回滚
├── risklist      # 黑白名单
├── profile       # 用户/设备/IP 风险画像查询
├── decision      # 实时决策接口、决策日志、事件落库
├── engine        # 规则执行、评分、决策解析
├── riskcase      # 人工审核案件与操作日志
├── dashboard     # 看板统计
├── audit         # 待实现
└── mq            # RabbitMQ 事件发布与消费
```

### 5.2 数据流

```text
业务请求
  -> RiskDecisionController
  -> RiskDecisionService
  -> ActiveStrategyLoader 加载已发布策略快照
  -> RiskContextBuilder 加载画像和名单，构建上下文
  -> DecisionEngine 执行规则表达式
  -> RiskScorer 汇总风险分
  -> DecisionResolver 解析最终决策
  -> DecisionPersistenceService 落事件、决策日志、命中规则
  -> RiskCaseService 在 REVIEW 时生成案件
  -> 返回 RiskDecisionResponse
  -> 事务提交后发布 risk.decision.created / risk.case.created
  -> ProfileUpdateConsumer 异步更新画像统计
```

### 5.3 数据库核心表

规则与策略：

```text
risk_rule
risk_rule_version
risk_strategy
risk_strategy_rule
risk_strategy_version
```

名单与画像：

```text
risk_list
risk_user_profile
risk_device_profile
risk_ip_profile
```

决策与案件：

```text
risk_event
risk_decision_log
risk_decision_hit_rule
risk_case
risk_case_operation_log
```

## 6. 重要文件修改记录

### 6.1 文档

- `docs/RiskGuard-MVP-开发冻结版设计.md`
  - MVP 范围、技术选型、建表 SQL、接口清单、开发顺序。
- `README.md`
  - 本地启动方式、接口索引、smoke test 命令。
- `docs/RiskGuard-项目上下文交接.md`
  - 当前文档，用于下次新会话恢复上下文。

### 6.2 基础设施

- `pom.xml`
  - Spring Boot、MyBatis-Plus、Redis、RabbitMQ、Aviator、Knife4j、JWT、Lombok。
- `docker-compose.yml`
  - MySQL、Redis、RabbitMQ。
  - 端口支持环境变量覆盖。
- `Dockerfile`
  - 后端容器构建。
- `application-docker.yml`
  - Docker 内部服务连接配置。
- `docker/mysql/init/001_schema.sql`
  - 所有 MVP 核心表。
- `docker/mysql/init/002_seed.sql`
  - 初始规则、策略、画像、名单、规则版本、策略快照。

### 6.3 通用能力

- `common/api`
  - `ApiResponse`、`PageResponse`、`ErrorCode`、健康检查。
- `common/exception`
  - `BusinessException`、`GlobalExceptionHandler`。
- `common/trace`
  - `TraceIdFilter`、`TraceIdHolder`。
- `common/cache/RiskCacheService.java`
  - Redis 规则/策略缓存、幂等缓存。
- `common/enums`
  - 决策、风险等级、规则动作、案件状态、名单类型等枚举。

### 6.4 业务模块

- `rule`
  - 规则 CRUD、发布、版本、表达式校验。
- `strategy`
  - 策略 CRUD、绑定规则、发布快照、版本、回滚。
- `risklist`
  - 黑白名单维护。
- `profile`
  - 用户/设备/IP 画像查询。
- `decision`
  - 实时决策、模拟决策、日志落库、日志查询。
- `engine`
  - 规则执行、上下文构建、评分、决策解析。
- `riskcase`
  - REVIEW 建案、案件流转、操作日志。
- `dashboard`
  - 总览统计、决策分布、规则命中排行、风险趋势、案件统计。
- `auth`
  - 登录、当前用户查询、数据库用户认证、JWT 签发。
- `mq`
  - RabbitMQ 配置、决策/案件事件发布、画像更新消费。
- `profile`
  - 用户/设备/IP 画像查询与异步画像统计更新。

### 6.5 测试

- `src/test/java/com/riskguard/engine/DecisionEngineTests.java`
  - 验证嵌套表达式执行和按分数解析决策。
- `src/test/java/com/riskguard/common/security/JwtServiceTests.java`
  - 验证 JWT 签发和核心声明解析。
- `src/test/java/com/riskguard/profile/ProfileAsyncUpdateServiceTests.java`
  - 验证决策事件驱动的画像增量更新。
- `scripts/smoke/run-smoke.ps1`
  - 端到端 smoke test，包含登录、受保护接口、Dashboard 和异步画像更新。
- `scripts/smoke/start-riskguard-smoke.ps1`
  - smoke test 后端启动脚本。

## 7. 已发现并修复的问题

### 7.1 Docker 端口冲突

问题：

- 本机已有 KnowFlow 容器占用 `13306`、`5672`、`15672` 等端口。

处理：

- `docker-compose.yml` 改为支持环境变量配置端口。
- Smoke test 使用 `23306`、`6380`、`5673`、`15673`。

### 7.2 MyBatis 字段映射错误

问题：

- `payCount24h` 默认映射成 `pay_count24h`，但数据库字段是 `pay_count_24h`。
- `failedLoginCount10m` 等字段同理。

处理：

- 在画像实体中使用 `@TableField` 显式指定字段：
  - `pay_count_24h`
  - `failed_login_count_10m`
  - `account_count_24h`
  - `payment_count_24h`
  - `login_count_10m`

### 7.3 Redis 误连本机已有服务

问题：

- 应用默认连 `localhost:6379`，本机该端口 Redis 需要认证，导致幂等缓存失败。

处理：

- Smoke test 改为启动并连接 RiskGuard 自己的 Redis：`6380 -> 6379`。
- `run-smoke.ps1` 和 `start-riskguard-smoke.ps1` 支持 `RedisPort` 参数。

### 7.4 后端端口冲突

问题：

- `8080` 被本机其他服务占用。

处理：

- Smoke test 使用后端端口 `28080`。

## 8. 当前待办事项

阶段 5 Dashboard、阶段 6 异步与画像更新、阶段 7 认证与权限均已完成。当前建议进入阶段 8 前端管理后台。

### 8.1 阶段 8：前端管理后台

建议技术栈：

- Vue 3
- TypeScript
- Element Plus
- ECharts

建议页面：

- 风控看板
- 规则管理
- 策略管理
- 名单管理
- 决策日志
- 案件工作台
- 案件详情

### 8.2 后续增强

- Elasticsearch 决策日志检索。
- AI 风险解释与案件摘要。
- 策略灰度真实路由。
- 开放接口签名校验。
- 操作审计日志。
- 单元测试和集成测试覆盖率提升。
- CI 流水线。

## 9. 下次会话建议起点

建议下次新会话从阶段 8 前端管理后台开始：

```text
请阅读 docs/RiskGuard-项目上下文交接.md，并继续实现阶段 8：前端管理后台。后端联调完成后必须运行 mvn test、mvn -DskipTests package 和 scripts/smoke/run-smoke.ps1。
```

阶段 8 推荐实现顺序：

1. 创建 Vue 3 + TypeScript 前端项目结构。
2. 接入登录页和 JWT token 存储。
3. 封装 API client，统一处理 `ApiResponse` 和 401/403。
4. 实现风控看板页面。
5. 实现规则、策略、名单管理页面。
6. 实现决策日志、案件工作台、案件详情页面。
7. 使用 ECharts 接入 Dashboard 数据。
8. 本地联调后更新 README。
9. 跑后端 `mvn test`、`mvn -DskipTests package` 和 smoke test。

## 10. Git 状态

当前分支：

```text
codex/riskguard-mvp
```

当前远程状态：

```text
origin/codex/riskguard-mvp
```

关键提交：

```text
097c39a docs: add RiskGuard MVP design
baf6fec feat: implement RiskGuard backend MVP
```

本交接文档创建后，需要单独提交。
