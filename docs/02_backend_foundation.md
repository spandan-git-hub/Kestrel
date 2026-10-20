# 02 — Backend Foundation (Phase 0)

Tasks: **P0-01 … P0-08, P0-14**. Everything here is reused by every service; keep libs small and boring (avoid a distributed monolith). Each microservice is a **Spring Boot Lambda handler** (AWS Lambda Java runtime); no long-running process, no Docker Compose in production.

## 1. Parent POM (`backend/pom.xml`)
- `packaging=pom`; modules: `libs/*`, `services/*` (add services only when their phase starts).
- Imports BOMs: `spring-boot-dependencies`, `spring-cloud-dependencies`, `testcontainers-bom`, `aws-lambda-java-events`, `software.amazon.awssdk` (DynamoDB, SQS, SNS, EventBridge). `⚠ VERIFY-01` — confirm compatible Boot ↔ Cloud versions and current artifact names from the official compatibility matrix before pinning.
- `java.version=21`, `maven.compiler.release=21`, UTF-8, `-parameters`.
- Plugins: `maven-enforcer` (Java 21+, banned duplicate deps, **ban `lombok`**), `spotless-maven-plugin` (google-java-format, check in `verify`), `jacoco` (80% on `*.domain.*`, `*.app.*`), `maven-surefire` + `maven-failsafe` (`*IT` = integration), `spring-boot-maven-plugin` (layered jars for Lambda packaging), `git-commit-id`.
- Dependency management for: resilience4j, springdoc, awaitility, wiremock, archunit, web-push (`nl.martijndwars:web-push` — verify current), jackson-dataformat-xml (cap-ingest/composer only), `aws-lambda-java-runtime-interface-client`, `aws-lambda-java-events`.
- Wrapper: `./mvnw` committed.

## 2. Libraries

### 2.1 `common-events` (P0-03)
- Java records for the **envelope** and every payload in doc 03 (`FloodSignal`, `HeatSignal`, `AqSignal`, `OfficialAlertSignal`, `ReportAccepted`, `ReportUpdated`, `AlertEvent`, `LocationUpserted`, `LocationDeleted`, `SubscriptionChanged`, `DeliveryReceipt`, `BroadcastCreated`, `FeedStatus`).
- Enums from master §8. Jackson config (`JavaTimeModule`, ISO instants, `FAIL_ON_UNKNOWN_PROPERTIES=false` for forward-compat, `WRITE_DATES_AS_TIMESTAMPS=false`).
- JSON Schemas under `src/main/resources/schemas/<EventType>.v1.json` + `EventSchemaValidator` used in tests (and optionally in dev profile on publish).
- Routing-key helpers: `RoutingKeys.signal(Hazard)`, `RoutingKeys.alert(Stage,Hazard)` — now map to SNS topic ARN suffixes and SQS queue name suffixes.
- **Acceptance:** round-trip serialization test for every event; schema validation passes for sample fixtures in `src/test/resources/fixtures/`.

### 2.2 `common-core` (P0-04)
- `ProblemDetail` global `@RestControllerAdvice` (RFC 7807 + `traceId`), standard exceptions (`NotFoundException`, `ConflictException`, `ValidationException`, `UpstreamUnavailableException`).
- `CorrelationIdFilter` (reads/writes `X-Request-Id`, MDC `traceId`), JSON logging config (`logback-spring.xml`, CloudWatch-friendly plain JSON layout — one JSON object per line, no ANSI).
- `Clock` bean + `TimeProvider` (inject everywhere; **never** `Instant.now()` in domain code).
- `PageResponse<T>`, `Ids` (UUID v4 / v7 optional), `GeoUtils` (haversine, bounding box, India bounds check lat 6–38, lon 68–98).
- `FeedStateTracker` (reusable HEALTHY/DEGRADED/DOWN transitions from consecutive failures + last-success age) with CloudWatch custom metrics `kestrel.feed.state`, `kestrel.ingest.data_age_seconds`.
- `CallBudget` (DynamoDB daily/minute counters with atomic conditional-write + fallback hook) used by Open-Meteo/OpenAQ clients.
- Auto-config via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.

### 2.3 `common-outbox` (P0-05)
Transactional outbox + idempotent inbox — adapted for DynamoDB.

- Per-service DynamoDB tables (created as part of the SAM/CDK template each service includes):
  - `kestrel_<service>_outbox`: `PK` (eventId), `exchange` (SNS topic suffix), `routingKey`, `priority`, `payload` (JSON string), `headers` (JSON), `createdAt` (ISO), `publishedAt`, `attempts`, `lastError`, TTL (auto-expire after 7 days once published).
  - `kestrel_<service>_inbox`: `PK` (eventId), `SK` (consumerName), `processedAt`, TTL (30 days).
- **Write path:** `OutboxPublisher.publish(EventEnvelope<?> e, String topicSuffix, String routingKey, Integer priority)` — must be called inside the same DynamoDB `TransactWriteItems` as the business item change.
- **Relay:** EventBridge Scheduler triggers an outbox-relay Lambda every 30 s (or inline before the function returns for latency-sensitive paths); queries DynamoDB for unpublished outbox items (`publishedAt` absent, `GSI` on `createdAt`), publishes to SNS with publisher-confirms equivalent (SNS is at-least-once; confirm via `PublishResult.messageId()`), marks `publishedAt`; exponential backoff on failure; metrics `kestrel.outbox.pending`, `kestrel.outbox.publish_failures`.
- `IdempotentHandler.handleOnce(eventId, consumerName, Runnable)` using DynamoDB conditional put on `inbox` table (skip if item already exists, same transaction as business change).
- **Consumer defaults:** Lambda SQS trigger with `batchSize` configurable (default 10), `bisectBatchOnFunctionError: true`, DLQ on the SQS queue (after 3 attempts); each queue maps to **one Lambda function** (own execution context → isolation guaranteed by Lambda).
- Tests: LocalStack (Testcontainers, DynamoDB + SQS + SNS): publish → simulate Lambda restart → delivered exactly once to inbox; duplicate delivery ignored; poison → DLQ.

### 2.4 `common-security` (P0-06)
- Cognito JWT filter (validates RS256 JWT from Cognito user pool via `NimbusJwtDecoder.withJwkSetUri`), claims: `sub`, `cognito:groups` (mapped to roles), `custom:locationIds`, `exp`. `⚠ VERIFY-25`.
- `@PreAuthorize` helpers: `hasRole('COORDINATOR')`, `@scopeCheck.canAccess(#locationId)`.
- `SubscriberTokenFilter` utility (hash = SHA-256 of `X-Subscriber-Token`) — used by registry and report-intake.
- Config property block `kestrel.security.cognito.jwk-uri`, `…issuer`, `…access-ttl`. Fail fast if JWK URI not set.
- `permitAll` allow-list is explicit per service (deny by default). CORS: configurable allowed origins (must match CloudFront/Amplify/Cloudflare Pages domain).
- Tests: expired/invalid/forged token → 401; wrong role → 403; scope enforcement.

### 2.5 `common-testing` (P0-07)
Base classes: `AbstractDynamoDbIT`, `AbstractSqsIT`, `AbstractSnsIT` (singleton LocalStack via Testcontainers, reuse), `WireMockSupport`, event fixture loader, `FixedClockConfig`, `Eventually` helpers (Awaitility).

### 2.6 ArchUnit rules (P0-08) — in `common-testing`, applied by each service
- `..domain..` must not depend on `org.springframework..`, `jakarta.persistence..`, `software.amazon.awssdk..`, `..infra..`.
- Services must not depend on other services' packages (`com.kestrel.<other>..`).
- Only `common-outbox` may use SnsClient/SqsClient for publishing.
- No `Instant.now()/LocalDate.now()/new Date()` in `..domain..` and `..app..`.

## 3. Service template (P0-14: `tools/new-service.sh <name> <port> <tablePrefix>`)
Generates: module + `pom.xml`, `Application` class + Lambda handler adapter, `application.yml` (port for local dev, DynamoDB endpoint, SQS/SNS endpoints, actuator, problem details), DynamoDB table definitions in SAM/CDK, SQS queue + DLQ, `README.md` skeleton, ArchUnit test, a health IT (LocalStack), and adds the SAM/CDK resource block. The script must be idempotent and refuse to overwrite.

### Standard `application.yml` skeleton
```yaml
server.port: ${PORT:8080}
spring:
  application.name: <service>
  jpa:    # only if Aurora DSQL is chosen for this service
    hibernate.ddl-auto: validate
    open-in-view: false
management.endpoints.web.exposure.include: health,info
management.endpoint.health.probes.enabled: true
kestrel:
  security.cognito.jwk-uri: ${COGNITO_JWK_URI}
  dynamodb.endpoint: ${DYNAMODB_ENDPOINT:}   # empty = AWS; set to LocalStack URL for local/test
  sqs.endpoint: ${SQS_ENDPOINT:}
  sns.endpoint: ${SNS_ENDPOINT:}
  table-prefix: ${TABLE_PREFIX:kestrel_<service>_}
```

## 4. Conventions cheat-sheet
- Controllers thin; use cases in `app`; no DynamoDB entities leaving `infra`; map with plain mappers (records).
- Use `Optional`/exceptions, never `null` returns from repositories' public ports.
- Money-style rounding not applicable; for probabilities use `double` fractions 0–1, round only at presentation.
- Scheduling: EventBridge Scheduler triggers Lambda at configured cron (UTC); no `@Scheduled` in Lambda handlers (function exits after invocation). Single-replica design guaranteed by Lambda (no shared in-memory state between invocations; use DynamoDB for state).
- Config: typed `@ConfigurationProperties` records, validated with `@Validated`.
- Package `com.kestrel.<service>.{api,app,domain,infra,config}`.
- Every public endpoint documented with springdoc annotations incl. error responses.
- Lambda cold-start: minimize `@Bean` initialization; use **SnapStart** for user-facing functions (`⚠ VERIFY-22`).
