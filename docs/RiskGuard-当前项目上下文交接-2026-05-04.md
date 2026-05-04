# RiskGuard 当前项目上下文交接

更新时间：2026-05-04  
建议下次会话入口：先阅读本文，再按“下一步建议”审查提交边界，并继续前端 CSS 命名整理或分组提交。

2026-05-04 晚间补充：阶段 8 前端总验收脚本已新增，前端构建、前端总验收、后端单测、后端打包和完整 smoke 均已通过。

## 1. 项目定位

RiskGuard 是一个面向登录、支付场景的实时风控决策 MVP。

核心目标：

- 支持规则、策略、名单等风控配置管理。
- 支持实时风险决策、规则命中、风险等级与处置结果输出。
- 支持决策日志、案件流转、操作记录、Dashboard 看板。
- 支持管理后台登录、JWT 鉴权、角色权限控制。
- 保持 MVP 范围收敛，暂缓 AI、Elasticsearch、Flink 等扩展项。

## 2. 关键技术与架构决策

### 2.1 后端

- 使用 Spring Boot 3 单体应用，包结构按未来服务边界组织。
- 数据库使用 MySQL，缓存使用 Redis，消息队列使用 RabbitMQ。
- API 返回统一使用 `ApiResponse<T>`，分页统一使用 `PageResponse<T>`。
- 管理端接口使用 JWT 鉴权和角色权限控制。
- `POST /api/v1/risk/decisions` 作为业务系统集成入口，MVP 阶段保持未鉴权。
- 实时决策主链路同步执行：构建上下文 -> 加载启用策略 -> 执行规则 -> 汇总风险分 -> 决策解析 -> 落库。
- RabbitMQ 用于决策创建、案件创建后的异步画像更新和后续扩展。
- Redis 做请求幂等缓存，数据库查询做幂等兜底。
- 案件只由 `REVIEW` 决策创建，案件支持 `PENDING -> PROCESSING -> APPROVED/REJECTED/CLOSED` 等状态变化。

### 2.2 前端

- 前端位于 `frontend/`，使用 Vue 3 + TypeScript + Vite。
- 图表使用 ECharts。
- 图标使用 `@element-plus/icons-vue`，未引入完整 Element Plus 组件库。
- 前端 API 统一封装在 `frontend/src/api/client.ts`。
- API 类型统一维护在 `frontend/src/api/types.ts`。
- 登录态存储在 `localStorage`：
  - `riskguard.accessToken`
  - `riskguard.user`
- Vite dev server 通过 proxy 转发 `/api` 到后端，默认目标为 `http://localhost:8080`。
- 管理后台风格走偏运营工具的紧凑布局，不做营销式首页。
- 当前前端验证策略：页面只读联调为主，浏览器验收不主动点击会改后端状态的按钮。

## 3. 当前运行环境参考

当前开发环境中已验证过：

- 后端：`http://localhost:8080`
- 前端：`http://127.0.0.1:5174/`
- 默认管理员：
  - username: `admin`
  - password: `RiskGuard@123456`
  - role: `ADMIN`

Docker 依赖常用端口：

- MySQL: `23306`
- Redis: `6380`
- RabbitMQ: `5673`
- RabbitMQ management: `15673`

注意：README 中仍写有 Vite 默认 `5173`，当前实际浏览器会话使用的是 `5174`，可能是因为 `5173` 已被占用。

## 4. 已完成阶段概览

### 阶段 0：项目骨架

已完成：

- Spring Boot 项目骨架。
- 基础包结构。
- Docker Compose 基础依赖。
- MySQL schema 与 seed。
- OpenAPI / Knife4j。
- 统一响应、异常处理、TraceId。

### 阶段 1：基础 CRUD

已完成：

- 风控规则 `risk_rule`。
- 风控策略 `risk_strategy`。
- 策略规则绑定 `risk_strategy_rule`。
- 黑白名单 `risk_list`。
- 用户、设备、IP 画像查询。

### 阶段 2：规则与策略发布

已完成：

- 规则发布与版本表。
- 策略发布与规则快照。
- 策略版本回滚。
- 规则表达式校验。
- 策略绑定规则时校验规则状态、版本和事件类型。

### 阶段 3：实时决策主链路

已完成：

- 正式决策接口。
- 模拟决策接口。
- 风控上下文构建。
- 规则表达式执行。
- 风险分汇总。
- 风险等级和最终决策解析。
- Redis 幂等缓存。

### 阶段 4：日志与案件闭环

已完成：

- 风控事件落库。
- 决策日志落库。
- 命中规则落库。
- `REVIEW` 决策自动创建案件。
- 案件查询、领取、审批、驳回、关闭、误报标记。
- 案件操作记录。

### 阶段 5：Dashboard 看板统计

已完成：

- 总览统计。
- 决策结果分布。
- 规则命中 Top 10。
- 24 小时风险趋势。
- 案件状态统计。

### 阶段 6：异步与画像更新

已完成：

- RabbitMQ exchange、queue、binding。
- 决策创建事件。
- 案件创建事件。
- 画像异步更新 consumer。
- 相关 smoke 覆盖。

### 阶段 7：认证与权限

已完成：

- 登录接口。
- 当前用户接口。
- JWT 生成、校验与 filter。
- `sys_user` 数据库用户认证。
- 角色权限控制。
- 管理 API 受保护。

### 阶段 8：前端管理后台

已完成页面：

- Login。
- Dashboard。
- Rules。
- Strategies。
- Lists。
- Decisions。
- Cases。

当前阶段 8 基本功能已齐，收口验收与全量回归已通过；建议下一步进入提交边界审查、CSS 命名整理或分组提交。

## 5. 阶段 8 前端页面状态

### Login

文件：

- `frontend/src/components/LoginView.vue`
- `frontend/src/App.vue`
- `frontend/src/api/client.ts`

能力：

- 登录。
- JWT 和用户信息持久化。
- 登录失败提示。
- 退出登录。

### Dashboard

文件：

- `frontend/src/components/DashboardView.vue`
- `frontend/src/components/EChartPanel.vue`

能力：

- 总览指标。
- 风险趋势折线图。
- 决策分布饼图。
- 规则命中排行。
- 案件状态图。
- 刷新按钮。

### Rules

文件：

- `frontend/src/components/RulesView.vue`

能力：

- 规则列表、筛选、分页。
- 规则详情。
- 创建、保存、启停。
- 表达式校验。
- 发布规则。
- 查看版本。

验收脚本：

- `scripts/smoke/run-rules-acceptance.ps1`

### Strategies

文件：

- `frontend/src/components/StrategyView.vue`

能力：

- 策略列表、筛选、分页。
- 策略详情。
- 创建、保存、启停。
- 绑定启用规则。
- 调整绑定顺序和启用状态。
- 发布策略。
- 查看版本和回滚入口。

注意：

- 删除绑定、回滚、发布等按钮会改后端数据，浏览器验收时没有实际点击。

验收脚本：

- `scripts/smoke/run-strategies-acceptance.ps1`

### Lists

文件：

- `frontend/src/components/ListsView.vue`

能力：

- 黑白名单列表。
- 按关键词、名单类型、对象类型、状态筛选。
- 名单详情。
- 创建、保存、启停、删除入口。
- 有效期展示。

验收脚本：

- `scripts/smoke/run-lists-acceptance.ps1`

### Decisions

文件：

- `frontend/src/components/DecisionsView.vue`

能力：

- 决策日志列表。
- 按 User ID、事件类型、决策结果筛选。
- 决策详情。
- 风险分、策略版本、traceId、原因展示。
- 命中规则表。

验收脚本：

- `scripts/smoke/run-decisions-acceptance.ps1`

### Cases

文件：

- `frontend/src/components/CasesView.vue`

能力：

- 案件列表。
- 按 User ID、案件状态筛选。
- 案件详情。
- 审计结果和审计意见展示。
- 操作记录表。
- Claim / Approve / Reject / False Positive / Close 入口。

注意：

- 处置按钮已接 API，并带 `window.confirm`。
- 浏览器验收没有点击这些会改变状态的按钮。

验收脚本：

- `scripts/smoke/run-cases-acceptance.ps1`

## 6. 重要文件修改记录

### 后端认证与权限

- `src/main/java/com/riskguard/auth/`
  - 登录、当前用户、认证 DTO 与 service。
- `src/main/java/com/riskguard/common/security/SecurityConfig.java`
  - JWT filter 接入。
  - 管理 API 权限控制。
  - 决策正式入口保持未鉴权。
- `src/main/java/com/riskguard/common/security/JwtAuthenticationFilter.java`
- `src/main/java/com/riskguard/common/security/JwtService.java`
- `src/main/java/com/riskguard/common/security/RiskUserPrincipal.java`
- `src/main/java/com/riskguard/common/security/SecurityUtils.java`

### Dashboard

- `src/main/java/com/riskguard/dashboard/`
  - Dashboard controller、service、mapper、DTO。
- `src/test/java/com/riskguard/dashboard/`
  - Dashboard service tests。

### 异步与画像

- `src/main/java/com/riskguard/mq/`
  - RabbitMQ constants、config、events、publisher、consumer。
- `src/main/java/com/riskguard/profile/service/ProfileAsyncUpdateService.java`
  - 决策事件后的用户、设备、IP 画像更新。

### 决策和案件

- `src/main/java/com/riskguard/decision/service/RiskDecisionService.java`
  - DB 幂等兜底。
  - 决策创建后发布事件。
- `src/main/java/com/riskguard/riskcase/service/RiskCaseService.java`
  - 案件创建事件。
  - 操作人优先取当前登录用户。

### 前端

- `frontend/package.json`
- `frontend/vite.config.ts`
- `frontend/src/App.vue`
- `frontend/src/main.ts`
- `frontend/src/styles.css`
- `frontend/src/api/client.ts`
- `frontend/src/api/types.ts`
- `frontend/src/components/LoginView.vue`
- `frontend/src/components/DashboardView.vue`
- `frontend/src/components/EChartPanel.vue`
- `frontend/src/components/RulesView.vue`
- `frontend/src/components/StrategyView.vue`
- `frontend/src/components/ListsView.vue`
- `frontend/src/components/DecisionsView.vue`
- `frontend/src/components/CasesView.vue`

### 验收脚本

- `scripts/smoke/run-frontend-acceptance.ps1`
- `scripts/smoke/run-dashboard-acceptance.ps1`
- `scripts/smoke/run-rules-acceptance.ps1`
- `scripts/smoke/run-strategies-acceptance.ps1`
- `scripts/smoke/run-lists-acceptance.ps1`
- `scripts/smoke/run-decisions-acceptance.ps1`
- `scripts/smoke/run-cases-acceptance.ps1`
- `scripts/smoke/run-smoke.ps1`

### 文档与配置

- `README.md`
  - 当前 API 和前端验收命令已更新。
- `.gitignore`
  - 已加入前端 npm cache 和本地 Maven cache 忽略。
- `docs/RiskGuard-项目上下文交接.md`
  - 老版交接文档，阶段 8 内容未完全反映当前最新状态。

## 7. 当前验证结果

最近已通过：

```powershell
npm run build
.\scripts\smoke\run-frontend-acceptance.ps1 -BaseUrl http://localhost:8080
.\scripts\smoke\run-dashboard-acceptance.ps1 -BaseUrl http://localhost:8080
.\scripts\smoke\run-rules-acceptance.ps1 -BaseUrl http://localhost:8080
.\scripts\smoke\run-strategies-acceptance.ps1 -BaseUrl http://localhost:8080
.\scripts\smoke\run-lists-acceptance.ps1 -BaseUrl http://localhost:8080
.\scripts\smoke\run-decisions-acceptance.ps1 -BaseUrl http://localhost:8080
.\scripts\smoke\run-cases-acceptance.ps1 -BaseUrl http://localhost:8080
mvn test
mvn -DskipTests package
.\scripts\smoke\run-smoke.ps1 -Port 28080 -MysqlPort 23306 -RedisPort 6380 -RabbitPort 5673
```

构建结果：

- `npm run build` 通过。
- Vite 仍有大 chunk warning，主要来自 ECharts 和当前单包构建，暂不影响功能。
- `mvn test` 通过：10 tests, 0 failures, 0 errors, 0 skipped。
- `mvn -DskipTests package` 通过，已生成 `target/riskguard-mvp-0.0.1-SNAPSHOT.jar`。
- 完整 smoke 通过，覆盖登录鉴权、规则/策略查询、画像查询、正式决策、幂等、决策日志、命中规则、`REVIEW` 自动建案、异步画像更新、案件 `claim -> approve` 和 Dashboard 聚合。
- 本轮完整 smoke 创建并审批测试案件：`C20260504212214bb1727a7`。

浏览器已验收：

- Login。
- Dashboard。
- Rules。
- Strategies。
- Lists。
- Decisions。
- Cases。

浏览器验收策略：

- 只读和筛选联动已验证。
- 未点击会修改后端状态的按钮，例如发布、删除、回滚、案件处置。

## 8. 当前已知限制与注意事项

- 前端多个页面复用了 `rules-workspace`、`rules-list-panel`、`rule-editor-panel` 等 CSS 类名，功能正常，但命名不够通用。
- 已新增统一的前端总验收脚本 `scripts/smoke/run-frontend-acceptance.ps1`，可串行运行 dashboard、rules、strategies、lists、decisions、cases acceptance 并输出汇总。
- 还没有做完整生产级前端路由，当前是单页面内通过 `activeView` 切换。
- 前端未引入完整 UI 组件库，所有表单、表格、按钮为自定义样式。
- 未做 token 自动刷新，JWT 过期后会清 session 并提示错误，需要重新登录。
- seed 数据中的中文在部分终端和浏览器快照中会显示乱码，疑似已有种子数据编码问题，不是本轮前端引入。
- 案件处置、规则发布、策略发布、名单删除等修改动作需要谨慎验收，避免污染当前共享数据库状态。
- 当前 PATH 中可能没有 `mvn`，本轮使用 IntelliJ 自带 Maven，并通过 `MAVEN_OPTS=-Dmaven.repo.local=.m2/repository` 将 Maven 本地仓库放在项目内；`.m2/` 已加入 `.gitignore`。
- 当前 worktree 有大量未提交文件和修改，下一步提交前应先确认分组和提交边界。

## 9. 当前待办事项

### 高优先级

1. 整理前端 CSS 命名。
   - 将 `rules-*` 类逐步抽成更通用的 `console-*` 或 `management-*`。
   - 保持小步修改，避免影响已验收页面。

2. 代码分组提交前审查。
   - 检查 `git status --short` 和 `git diff --stat`。
   - 确认后端阶段 5/6/7、前端阶段 8、验收脚本和文档的提交边界。

### 中优先级

1. 给 Cases 做受控的处置验收。
   - 需要专门创建一次测试案件。
   - 然后验证 claim -> approve 或 reject。
   - 该操作会写数据库，应在明确允许后执行。

2. 给 Rules / Strategies / Lists 做受控的写入验收。
   - 创建临时规则、发布、绑定、删除等。
   - 建议使用 `ACCEPT_` 前缀测试数据，便于识别。

3. 前端体验整理。
   - 空态。
   - 错误态。
   - 表格列宽。
   - 窄屏布局。
   - 长文本换行。

4. 代码分组提交。
   - 后端阶段 5/6/7。
   - 前端阶段 8。
   - 验收脚本和文档。

### 低优先级

1. 前端代码拆包。
   - 使用动态 import 或 manualChunks 降低 Vite chunk warning。

2. 增加前端单元测试或 Playwright 测试。

3. 接入真实路由。

4. 增加更细的权限体验，例如菜单按角色显示。

## 10. 建议下次会话起点

建议直接使用下面这段作为新会话 prompt：

```text
请阅读 docs/RiskGuard-当前项目上下文交接-2026-05-04.md，继续 RiskGuard 项目。阶段 8 前端页面已经完成到 Cases，scripts/smoke/run-frontend-acceptance.ps1 已新增，npm run build、前端总验收、mvn test、mvn -DskipTests package 和完整 smoke 已通过。下一步请先审查 git status / diff 边界，再进行前端 CSS 命名整理或代码分组提交。暂时不要进行会修改数据库状态的浏览器操作，除非明确创建 ACCEPT_ 前缀测试数据。
```

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
npm run build
```

前端分页面验收：

```powershell
.\scripts\smoke\run-dashboard-acceptance.ps1 -BaseUrl http://localhost:8080
.\scripts\smoke\run-rules-acceptance.ps1 -BaseUrl http://localhost:8080
.\scripts\smoke\run-strategies-acceptance.ps1 -BaseUrl http://localhost:8080
.\scripts\smoke\run-lists-acceptance.ps1 -BaseUrl http://localhost:8080
.\scripts\smoke\run-decisions-acceptance.ps1 -BaseUrl http://localhost:8080
.\scripts\smoke\run-cases-acceptance.ps1 -BaseUrl http://localhost:8080
```

前端总验收：

```powershell
.\scripts\smoke\run-frontend-acceptance.ps1 -BaseUrl http://localhost:8080
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

## 12. 当前工作树提醒

当前存在未提交修改和未跟踪文件。不要随意执行 `git reset --hard` 或 `git checkout --`。

提交前建议先审查：

```powershell
git status --short
git diff --stat
```

本轮新增前端目录 `frontend/` 当前整体未跟踪，`git diff --stat` 不会完整展示其内部所有新增文件。
