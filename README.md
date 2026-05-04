# RiskGuard MVP

RiskGuard is a realtime risk decision platform MVP for login and payment scenarios.

## Current Scope

- Spring Boot 3 backend skeleton
- Unified API response and global exception handling
- TraceId filter and log pattern
- OpenAPI / Knife4j configuration
- JWT login and role-based management API protection
- MySQL, Redis, RabbitMQ Docker Compose infrastructure
- RabbitMQ risk events and async profile updates
- Vue 3 management console for login, Dashboard, rules, strategies, lists, decisions and cases
- MVP database schema and seed data

## Local Development

Start infrastructure:

```bash
docker compose up -d mysql redis rabbitmq
```

Run backend locally:

```bash
mvn spring-boot:run
```

Health check:

```bash
curl http://localhost:8080/api/v1/health
```

Knife4j:

```text
http://localhost:8080/doc.html
```

Run frontend console:

```bash
cd frontend
npm install
npm run dev
```

The frontend dev server runs at:

```text
http://127.0.0.1:5173
```

If `5173` is occupied, Vite may pick the next available port such as `5174`.

Its API proxy defaults to `http://localhost:8080`. If you run the smoke-test backend on `28080`, start Vite with:

```powershell
$env:VITE_API_TARGET = 'http://localhost:28080'
npm run dev
```

## MVP Phase 1 APIs

- `POST /api/v1/auth/login`
- `GET /api/v1/auth/me`
- `POST /api/v1/rules`
- `PUT /api/v1/rules/{id}`
- `GET /api/v1/rules`
- `GET /api/v1/rules/{id}`
- `POST /api/v1/rules/{id}/enable`
- `POST /api/v1/rules/{id}/disable`
- `POST /api/v1/rules/{id}/publish`
- `GET /api/v1/rules/{id}/versions`
- `POST /api/v1/rules/validate-expression`
- `POST /api/v1/strategies`
- `PUT /api/v1/strategies/{id}`
- `GET /api/v1/strategies`
- `GET /api/v1/strategies/{id}`
- `POST /api/v1/strategies/{id}/enable`
- `POST /api/v1/strategies/{id}/disable`
- `POST /api/v1/strategies/{id}/rules`
- `GET /api/v1/strategies/{id}/rules`
- `DELETE /api/v1/strategies/{id}/rules/{ruleId}`
- `PUT /api/v1/strategies/{id}/rules/order`
- `POST /api/v1/strategies/{id}/publish`
- `POST /api/v1/strategies/{id}/rollback`
- `GET /api/v1/strategies/{id}/versions`
- `POST /api/v1/risk-lists`
- `PUT /api/v1/risk-lists/{id}`
- `GET /api/v1/risk-lists`
- `GET /api/v1/risk-lists/{id}`
- `POST /api/v1/risk-lists/{id}/enable`
- `POST /api/v1/risk-lists/{id}/disable`
- `DELETE /api/v1/risk-lists/{id}`
- `GET /api/v1/profiles/users/{userId}`
- `GET /api/v1/profiles/devices/{deviceId}`
- `GET /api/v1/profiles/ips/{ip}`
- `POST /api/v1/risk/decisions`
- `POST /api/v1/risk/decisions/simulate`
- `GET /api/v1/risk/decisions`
- `GET /api/v1/risk/decisions/{decisionNo}`
- `GET /api/v1/risk/decisions/{decisionNo}/hit-rules`
- `GET /api/v1/cases`
- `GET /api/v1/cases/{caseNo}`
- `POST /api/v1/cases/{caseNo}/claim`
- `POST /api/v1/cases/{caseNo}/approve`
- `POST /api/v1/cases/{caseNo}/reject`
- `POST /api/v1/cases/{caseNo}/close`
- `POST /api/v1/cases/{caseNo}/mark-false-positive`
- `GET /api/v1/cases/{caseNo}/operations`
- `GET /api/v1/dashboard/overview`
- `GET /api/v1/dashboard/decision-distribution`
- `GET /api/v1/dashboard/rule-hit-rank`
- `GET /api/v1/dashboard/risk-trend`
- `GET /api/v1/dashboard/case-statistics`

Default admin account:

```text
username: admin
password: RiskGuard@123456
role: ADMIN
```

Management APIs require:

```text
Authorization: Bearer <accessToken>
```

`POST /api/v1/risk/decisions` remains unauthenticated for MVP business-system integration.

Example decision request:

```json
{
  "requestNo": "REQ202605010001",
  "eventType": "PAYMENT",
  "userId": "U10001",
  "deviceId": "D90001",
  "ip": "192.168.1.10",
  "amount": 1299.00,
  "bizId": "ORDER202605010001",
  "scene": "APP",
  "eventTime": "2026-05-01 10:30:00",
  "extra": {
    "payMethod": "BANK_CARD"
  }
}
```

Backend regression:

```powershell
mvn test
mvn -DskipTests package
.\scripts\smoke\run-smoke.ps1 -Port 28080 -MysqlPort 23306 -RedisPort 6380 -RabbitPort 5673
```

Frontend build and aggregate acceptance check:

```powershell
cd frontend
npm run build
cd ..
.\scripts\smoke\run-frontend-acceptance.ps1 -BaseUrl http://localhost:8080
```

Login and Dashboard acceptance check:

```powershell
./scripts/smoke/run-dashboard-acceptance.ps1 -BaseUrl http://localhost:8080
```

Rules acceptance check:

```powershell
./scripts/smoke/run-rules-acceptance.ps1 -BaseUrl http://localhost:8080
```

Strategies acceptance check:

```powershell
./scripts/smoke/run-strategies-acceptance.ps1 -BaseUrl http://localhost:8080
```

Lists acceptance check:

```powershell
./scripts/smoke/run-lists-acceptance.ps1 -BaseUrl http://localhost:8080
```

Decisions acceptance check:

```powershell
./scripts/smoke/run-decisions-acceptance.ps1 -BaseUrl http://localhost:8080
```

Cases acceptance check:

```powershell
./scripts/smoke/run-cases-acceptance.ps1 -BaseUrl http://localhost:8080
```

RabbitMQ management:

```text
http://localhost:15673
username: riskguard
password: riskguard123
```

RabbitMQ topology:

```text
exchange: riskguard.events
routing:  risk.decision.created -> riskguard.profile.update, riskguard.dashboard.aggregate
routing:  risk.case.created     -> riskguard.dashboard.aggregate
```

To run backend in Docker after the project can package successfully:

```bash
docker compose --profile app up -d
```
