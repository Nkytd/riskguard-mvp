# RiskGuard 当前项目上下文交接

更新时间：2026-05-05（二次更新：PR 创建、自查与前端增强后）
当前开发分支：`codex/riskguard-mvp`
当前最新实现提交：`6faa97b build: split frontend vendor chunks`（本文档更新提交以 `git log -1` 为准）
基线分支：`main`，当前指向 `baf6fec feat: implement RiskGuard backend MVP`
远端仓库：`https://github.com/Nkytd/riskguard-mvp.git`

建议下次新会话入口：先阅读本文，再按“下一步建议”继续处理 PR 最终合并、权限口径、MQ 重试/DLQ、Redis 缓存覆盖或前端路由等后续优化。

## 1. 项目定位

RiskGuard 是一个面向登录、支付场景的实时风控决策 MVP。

核心目标：

- 支持规则、策略、名单等风控配置管理。
- 支持实时风险决策、规则命中、风险等级与处置结果输出。
- 支持决策日志、案件流转、操作记录、Dashboard 看板。
- 支持管理后台登录、JWT 鉴权、角色权限控制。
- 支持 RabbitMQ 事件发布与异步画像更新。
- 保持 MVP 范围收敛，暂缓 AI、Elasticsearch、Flink、复杂特征平台等扩展项。

## 2. 整体架构思路

### 2.1 后端架构

- 使用 Spring Boot 3 模块化单体应用，包结构按未来服务边界组织。
- 数据库使用 MySQL，缓存使用 Redis，消息队列使用 RabbitMQ。
- API 返回统一使用 `ApiResponse<T>`，分页统一使用 `PageResponse<T>`。
- 管理端 API 使用 JWT 鉴权和角色权限控制。
- `POST /api/v1/risk/decisions` 作为业务系统集成入口，MVP 阶段保持未鉴权。
- 实时决策主链路同步执行：构建上下文 -> 加载启用策略 -> 执行规则 -> 汇总风险分 -> 决策解析 -> 落库。
- RabbitMQ 用于决策创建、案件创建后的异步画像更新和后续扩展。
- Redis 做请求幂等缓存和 active strategy cache；数据库查询做幂等兜底。
- 案件只由 `REVIEW` 决策创建，案件支持 `PENDING -> PROCESSING -> APPROVED/REJECTED/CLOSED` 等状态变化。

### 2.2 决策链路

```text
业务事件
  -> RiskDecisionService.normalize
  -> RiskCacheService 幂等查询
  -> ActiveStrategyLoader 加载策略快照
  -> RiskContextBuilder 构建上下文
  -> DecisionEngine 执行规则表达式
  -> DecisionResolver 计算风险分、风险等级与决策
  -> DecisionPersistenceService 落事件、决策、命中规则
  -> RiskCaseService 针对 REVIEW 自动建案
  -> RiskEventPublisher 发布决策/案件事件
  -> ProfileUpdateConsumer 异步更新用户/设备/IP 画像
```

### 2.3 前端架构

- 前端位于 `frontend/`，使用 Vue 3 + TypeScript + Vite。
- 图表使用 ECharts。
- 图标使用 `@element-plus/icons-vue`，未引入完整 Element Plus 组件库。
- API 统一封装在 `frontend/src/api/client.ts`。
- API 类型统一维护在 `frontend/src/api/types.ts`。
- API 错误文案通过 `formatApiError` 统一处理，401/403 会清理本地 session。
- 空态/错误态统一使用 `frontend/src/components/StateBlock.vue`。
- 前端单测使用 Vitest，当前重点覆盖 API client 的 session、鉴权、查询参数和错误归一化。
- ECharts 已改为按需注册，并通过 Vite `manualChunks` 拆出 `vue-vendor`、`element-icons`、`echarts-vendor`。
- 登录态存储在 `localStorage`：
  - `riskguard.accessToken`
  - `riskguard.user`
- 当前前端是单页管理后台，通过 `activeView` 切换视图，尚未引入真实路由。
- 管理后台设计走运营工具风格：紧凑、可扫描、偏工作台，不做营销式首页。

## 3. 关键技术与产品决策

- MVP 阶段不引入 AI、Flink、Elasticsearch，避免范围失控。
- 管理端 API 受 JWT 保护；正式决策入口保持未鉴权，便于业务系统集成。
- 规则发布后生成 `risk_rule_version`。
- 策略绑定的是规则版本，策略发布后生成 `risk_strategy_version`，并保存规则快照 JSON。
- 决策执行使用已发布策略快照，避免规则编辑污染历史决策。
- Redis active strategy cache 在策略更新、启停、绑定、解绑、排序时需要清理。
- 完整 smoke 可以写入测试案件；前端只读验收不主动点击发布、删除、回滚、案件处置等改状态按钮。
- 受控写入验收统一使用 `ACCEPT_` 前缀，脚本结束后禁用临时规则/策略、删除临时名单，案件保留为审计记录。

## 4. 已完成阶段

### 阶段 0：项目骨架

- Spring Boot 项目骨架。
- 基础包结构。
- Docker Compose 基础依赖。
- MySQL schema 与 seed。
- OpenAPI / Knife4j。
- 统一响应、异常处理、TraceId。

### 阶段 1：基础 CRUD

- 风控规则 `risk_rule`。
- 风控策略 `risk_strategy`。
- 策略规则绑定 `risk_strategy_rule`。
- 黑白名单 `risk_list`。
- 用户、设备、IP 画像查询。

### 阶段 2：规则与策略发布

- 规则发布与版本表。
- 策略发布与规则快照。
- 策略版本回滚。
- 规则表达式校验。
- 策略绑定规则时校验规则状态、版本和事件类型。

### 阶段 3：实时决策主链路

- 正式决策接口。
- 模拟决策接口。
- 风控上下文构建。
- 规则表达式执行。
- 风险分汇总。
- 风险等级和最终决策解析。
- Redis 幂等缓存。

### 阶段 4：日志与案件闭环

- 风控事件落库。
- 决策日志落库。
- 命中规则落库。
- `REVIEW` 决策自动创建案件。
- 案件查询、领取、审批、驳回、关闭、误报标记。
- 案件操作记录。

### 阶段 5：Dashboard 看板统计

- 总览统计。
- 决策结果分布。
- 规则命中 Top 10。
- 24 小时风险趋势。
- 案件状态统计。
- Dashboard service tests。

### 阶段 6：异步与画像更新

- RabbitMQ exchange、queue、binding。
- 决策创建事件。
- 案件创建事件。
- 画像异步更新 consumer。
- `ProfileAsyncUpdateService` 测试覆盖。
- 完整 smoke 覆盖异步画像更新。

### 阶段 7：认证与权限

- 登录接口。
- 当前用户接口。
- JWT 生成、校验与 filter。
- `sys_user` 数据库用户认证。
- 角色权限控制。
- 管理 API 受保护。
- JWT service tests。

### 阶段 8：前端管理后台

已完成页面：

- Login。
- Dashboard。
- Rules。
- Strategies。
- Lists。
- Decisions。
- Cases。

已完成收口：

- 新增前端总验收脚本。
- CSS 类名从 `rules-*` 泛化为 `management-*`。
- 增强表格、详情、长文本、窄屏换行体验。
- 系统化前端空态和错误态，新增统一 `StateBlock`。
- 新增 Vitest 前端单元测试，沉淀 API client 基础行为测试资产。
- ECharts 按需导入并通过 Vite manualChunks 拆包，当前 `npm run build` 已无大 chunk warning。
- 前端 build 和总验收通过。

## 5. 当前运行环境参考

已验证环境：

- 后端：`http://localhost:8080`
- 前端：`http://127.0.0.1:5174/`，`5173` 被占用时 Vite 会自动使用后续端口。
- 默认管理员：
  - username: `admin`
  - password: `RiskGuard@123456`
  - role: `ADMIN`

Docker 依赖常用端口：

- MySQL: `23306`
- Redis: `6380`
- RabbitMQ: `5673`
- RabbitMQ management: `15673`

注意：

- 当前 PATH 里可能没有 `mvn`。本轮使用 IntelliJ 自带 Maven：
  `C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.3\plugins\maven\lib\maven3\bin\mvn.cmd`
- Maven 本地仓库使用项目内 `.m2/repository`，`.m2/` 已加入 `.gitignore`。
- GitHub 推送需要走本机代理 `127.0.0.1:7890`。

## 6. 当前验证结果

最近已通过：

```powershell
cd frontend
npm test
npm run build
cd ..

.\scripts\smoke\run-frontend-acceptance.ps1 -BaseUrl http://localhost:8080
.\scripts\smoke\run-write-acceptance.ps1 -BaseUrl http://localhost:8080

$env:MAVEN_OPTS='-Dmaven.repo.local=.m2/repository'
& "C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.3\plugins\maven\lib\maven3\bin\mvn.cmd" test
& "C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.3\plugins\maven\lib\maven3\bin\mvn.cmd" -DskipTests package

.\scripts\smoke\run-smoke.ps1 -Port 28080 -MysqlPort 23306 -RedisPort 6380 -RabbitPort 5673
```

验证结论：

- `npm test` 通过：1 file, 8 tests passed，覆盖 API client session、鉴权、查询参数和错误归一化。
- `npm run build` 通过；ECharts 已按需注册并拆包，当前无 Vite 大 chunk warning。
- 前端总验收通过，覆盖 dashboard、rules、strategies、lists、decisions、cases。
- 受控写入验收通过，覆盖 Rules / Strategies / Lists / Cases 写操作。
- `mvn test` 通过：10 tests, 0 failures, 0 errors, 0 skipped。
- `mvn -DskipTests package` 通过。
- 完整 smoke 通过，覆盖登录鉴权、规则/策略查询、画像查询、正式决策、幂等、决策日志、命中规则、`REVIEW` 自动建案、异步画像更新、案件 `claim -> approve` 和 Dashboard 聚合。

## 7. 重要文件修改记录

### 7.1 后端认证与权限

- `src/main/java/com/riskguard/auth/`
  - 登录、当前用户、认证 DTO、`SysUser`、mapper、service。
- `src/main/java/com/riskguard/common/security/SecurityConfig.java`
  - JWT filter 接入。
  - 管理 API 权限控制。
  - 决策正式入口保持未鉴权。
  - 统一 401 / 403 JSON 响应。
- `src/main/java/com/riskguard/common/security/JwtAuthenticationFilter.java`
- `src/main/java/com/riskguard/common/security/JwtService.java`
- `src/main/java/com/riskguard/common/security/RiskUserPrincipal.java`
- `src/main/java/com/riskguard/common/security/SecurityUtils.java`
- `src/test/java/com/riskguard/common/security/JwtServiceTests.java`

### 7.2 Dashboard

- `src/main/java/com/riskguard/dashboard/`
  - Dashboard controller、service、mapper、DTO。
- `src/test/java/com/riskguard/dashboard/DashboardServiceTests.java`

### 7.3 异步与画像

- `src/main/java/com/riskguard/mq/`
  - RabbitMQ constants、config、events、publisher、consumer。
- `src/main/java/com/riskguard/profile/service/ProfileAsyncUpdateService.java`
  - 决策事件后的用户、设备、IP 画像更新。
- `src/test/java/com/riskguard/profile/ProfileAsyncUpdateServiceTests.java`

### 7.4 决策和案件

- `src/main/java/com/riskguard/decision/service/RiskDecisionService.java`
  - DB 幂等兜底。
  - 决策创建后发布事件。
- `src/main/java/com/riskguard/riskcase/service/RiskCaseService.java`
  - 案件创建事件。
  - 操作人优先取当前登录用户。

### 7.5 策略缓存修复

- `src/main/java/com/riskguard/strategy/service/RiskStrategyService.java`
  - 策略更新时清理旧/新 eventType active strategy cache。
  - 策略启停时清理 active strategy cache。
  - 空绑定策略查询返回空列表，避免 `selectBatchIds(empty)` 触发异常。

### 7.6 前端

- `frontend/package.json`
- `frontend/vite.config.ts`
- `frontend/src/App.vue`
- `frontend/src/main.ts`
- `frontend/src/styles.css`
- `frontend/src/api/client.ts`
- `frontend/src/api/types.ts`
- `frontend/src/api/client.test.ts`
- `frontend/src/components/LoginView.vue`
- `frontend/src/components/DashboardView.vue`
- `frontend/src/components/EChartPanel.vue`
- `frontend/src/components/StateBlock.vue`
- `frontend/src/components/RulesView.vue`
- `frontend/src/components/StrategyView.vue`
- `frontend/src/components/ListsView.vue`
- `frontend/src/components/DecisionsView.vue`
- `frontend/src/components/CasesView.vue`

### 7.7 验收脚本

- `scripts/smoke/run-dashboard-acceptance.ps1`
- `scripts/smoke/run-rules-acceptance.ps1`
- `scripts/smoke/run-strategies-acceptance.ps1`
- `scripts/smoke/run-lists-acceptance.ps1`
- `scripts/smoke/run-decisions-acceptance.ps1`
- `scripts/smoke/run-cases-acceptance.ps1`
- `scripts/smoke/run-frontend-acceptance.ps1`
- `scripts/smoke/run-write-acceptance.ps1`
- `scripts/smoke/run-smoke.ps1`

### 7.8 文档与配置

- `README.md`
  - API、前端、验收命令、RabbitMQ management 端口、受控写入验收说明。
- `.gitignore`
  - 忽略 `.m2/`、前端 npm cache、构建产物和日志。
- `docs/RiskGuard-当前项目上下文交接-2026-05-04.md`
  - 上一版交接文档。
- `docs/RiskGuard-当前项目上下文交接-2026-05-05.md`
  - 当前最新版交接文档。

## 8. Git 与 PR 状态

当前分支：

```text
main                baf6fec feat: implement RiskGuard backend MVP
codex/riskguard-mvp 6faa97b build: split frontend vendor chunks
```

远端已推送：

- `origin/main`
- `origin/codex/riskguard-mvp`

PR 状态：

- PR 已创建：`https://github.com/Nkytd/riskguard-mvp/pull/1`
- base: `main`
- compare: `codex/riskguard-mvp`
- 最近一次确认：head 为 `6faa97b`，`mergeable_state = clean`

注意：

- 当前本机没有 `gh` / `hub` CLI，PR 需要在 GitHub 页面创建。
- `main` 是基线分支，指向 `baf6fec`，用于让 PR 展示阶段 5-8、验收脚本和体验整理的完整 diff。

## 9. 已知限制与注意事项

- 前端尚未接入真实路由，当前通过 `activeView` 切换。
- 前端未引入完整 UI 组件库，表单、表格、按钮为自定义样式。
- 未做 token 自动刷新，JWT 过期后会清 session 并提示错误，需要重新登录。
- seed 数据中的中文在部分终端和浏览器快照中可能显示乱码，疑似已有种子数据编码问题，不是前端引入。
- `http://127.0.0.1:8080/` 是后端根路径，不是前端入口，未鉴权访问会返回 401；前端入口是 Vite 端口，如 `http://127.0.0.1:5174/`。
- ECharts vendor chunk 约 545KB，已通过按需导入和 `manualChunks` 拆出独立包；当前 `chunkSizeWarningLimit=600`，构建无大 chunk warning。
- 对照冻结版设计仍有工程化增强空间：
  - `AUDITOR` 是否允许处理案件需明确；当前案件写操作只允许 `ADMIN` / `RISK_OPERATOR`。
  - Redis 目前主要覆盖幂等和 active strategy cache，名单/画像缓存尚未完全按冻结版落地。
  - RabbitMQ 已发布决策/案件事件并异步更新画像，但 dashboard aggregate 队列目前是预留绑定，Dashboard 仍走实时 SQL 聚合。
  - MQ 消费失败重试/DLQ 未显式配置，当前主要依赖 Spring AMQP 默认行为。
  - `risk_list.effect_type` 已建模，但决策链路主要通过规则表达式读取 `list.*Hit`，尚未形成通用名单加减分/放行机制。
- 受控写入验收会写数据库：
  - 创建并禁用 `ACCEPT_` 规则和策略。
  - 创建并删除 `ACCEPT_` 名单。
  - 创建并处置 `ACCEPT_` 案件，案件保留为审计记录。
- 完整 smoke 也会创建并审批测试案件。

## 10. 当前待办事项

### 高优先级

1. 在 GitHub 页面最终 review / merge PR #1：
   - PR：`https://github.com/Nkytd/riskguard-mvp/pull/1`
   - base: `main`
   - compare: `codex/riskguard-mvp`
   - merge 前建议确认 PR 页面 checks / Files changed 无异常。

2. 明确 `AUDITOR` 角色口径：
   - 如果 AUDITOR 是审核员，应允许案件 claim/approve/reject/close。
   - 如果 AUDITOR 是只读审计员，应更新命名或文档，避免和“审核员可以处理案件”冲突。

3. 为 MQ 增加显式重试 / DLQ 配置：
   - profile update consumer 失败可重试。
   - 后续 dashboard aggregate consumer 接入时也复用相同策略。

### 中优先级

1. 补 Redis 名单/画像缓存，严格对齐冻结版 Redis Key 设计。
2. 接入 dashboard aggregate consumer 或明确 Dashboard MVP 使用实时 SQL 聚合。
3. 角色体验细化，例如菜单按角色显示、按钮按权限隐藏。
4. 优化表格列宽和移动端交互细节。
5. 增加 Playwright 浏览器级 smoke，覆盖登录和主要页面首屏渲染。

### 低优先级

1. 接入真实前端路由。
2. 做更细的权限模型和审计展示。
3. 规划非 MVP 扩展：AI、Elasticsearch、Flink、特征服务等。

## 11. 常用命令

启动依赖：

```powershell
docker compose -f .\docker-compose.yml up -d mysql redis rabbitmq
```

启动后端：

```powershell
mvn spring-boot:run
```

启动前端：

```powershell
cd frontend
npm run dev
```

前端构建：

```powershell
cd frontend
npm test
npm run build
```

前端总验收：

```powershell
.\scripts\smoke\run-frontend-acceptance.ps1 -BaseUrl http://localhost:8080
```

受控写入验收：

```powershell
.\scripts\smoke\run-write-acceptance.ps1 -BaseUrl http://localhost:8080
```

后端回归：

```powershell
mvn test
mvn -DskipTests package
```

完整 smoke：

```powershell
mvn -q -DskipTests package
.\scripts\smoke\run-smoke.ps1 -Port 28080 -MysqlPort 23306 -RedisPort 6380 -RabbitPort 5673
```

本环境 Maven 替代命令：

```powershell
$env:MAVEN_OPTS='-Dmaven.repo.local=.m2/repository'
& "C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.3\plugins\maven\lib\maven3\bin\mvn.cmd" test
& "C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.3\plugins\maven\lib\maven3\bin\mvn.cmd" -DskipTests package
```

走代理推送：

```powershell
git -c http.proxy=http://127.0.0.1:7890 -c https.proxy=http://127.0.0.1:7890 push
```

## 12. 下次会话推荐 Prompt

```text
请阅读 docs/RiskGuard-当前项目上下文交接-2026-05-05.md，继续 RiskGuard 项目。
当前 main 是基线分支，指向 baf6fec；codex/riskguard-mvp 是开发分支，最新实现提交 6faa97b，已推送到 origin。
PR #1 已创建：https://github.com/Nkytd/riskguard-mvp/pull/1，最近确认 mergeable_state=clean。
阶段 5 Dashboard、阶段 6 异步画像、阶段 7 认证权限、阶段 8 前端管理后台、前端空态/错误态、Vitest 前端单测、Vite 拆包、前端总验收、受控写入验收和完整 smoke 都已完成。
下一步请优先处理：PR 最终 review/merge、AUDITOR 案件权限口径、RabbitMQ 重试/DLQ、Redis 名单/画像缓存，或 dashboard aggregate consumer。
```
