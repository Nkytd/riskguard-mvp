# RiskGuard MVP

RiskGuard is a realtime risk decision platform MVP for login and payment scenarios.

## Current Scope

- Spring Boot 3 backend skeleton
- Unified API response and global exception handling
- TraceId filter and log pattern
- OpenAPI / Knife4j configuration
- Basic stateless Spring Security configuration
- MySQL, Redis, RabbitMQ Docker Compose infrastructure
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

## MVP Phase 1 APIs

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

Smoke test:

```bash
./scripts/smoke/run-smoke.ps1 -Port 28080 -MysqlPort 23306 -RedisPort 6380 -RabbitPort 5673
```

RabbitMQ management:

```text
http://localhost:15672
username: riskguard
password: riskguard123
```

To run backend in Docker after the project can package successfully:

```bash
docker compose --profile app up -d
```
