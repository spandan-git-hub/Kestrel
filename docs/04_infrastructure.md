# 04 — Infrastructure, Observability, CI/CD, Deployment

Tasks: **P0-09 … P0-13, P0-20, P1-36, P1-50, P4-xx**. AWS Lambda + serverless (no VM, no Docker Compose in production). `⚠ VERIFY-14`: re-check AWS Amplify Hosting and Cloudflare Pages free-tier terms before relying on them. `⚠ VERIFY-19,20,21,22,24,26`: verify all AWS free-tier limits.

## 1. Infrastructure as Code (`infra/`)
```
infra/
├─ sam-template.yaml            (or cdk/ — decided in D-22; single file declares all AWS resources)
├─ .env.example
├─ dynamodb/                    (local table schemas for LocalStack dev)
├─ sqs-sns/                     (queue/topic definitions mirrored from SAM for documentation)
├─ cognito/                     (user pool config; exported from SAM)
├─ cloudwatch/                  (dashboard JSON + alarm definitions; deployed via SAM)
├─ seed/locations.csv           (≥10 sample monitored points incl. Beed, Maharashtra district; marked UNVERIFIED snap)
└─ scripts/{backup.sh,restore.sh,deploy.sh,gen-vapid-keys.sh,gen-secret.sh}
```

### SAM/CDK resource groups
| Group | Resources |
|---|---|
| `core` | DynamoDB tables (all services), SQS queues + DLQs, SNS topics, EventBridge Scheduler group, Cognito user pool + client, CloudFront distribution, S3 bucket (CAP feed + backups) |
| `mvp-lambdas` | subscriber-registry, flood-ingest, rules-engine, alert-fanout Lambda functions + EventBridge triggers |
| `phase2-lambdas` | cap-ingest, report-intake, signal-fusion Lambda functions |
| `phase3-lambdas` | heat-ingest, aq-ingest, alert-composer, audit-evaluation Lambda functions |
| `observability` | CloudWatch dashboards, alarms, log groups, metric filters |

Deploy: `sam deploy --parameter-overrides Env=prod ...` (or `cdk deploy`).
Each Lambda: `Runtime: java21`, `MemorySize` per §2, `Timeout` per function type, `SnapStart: GeneralAvailability` for user-facing functions (`⚠ VERIFY-22`), `Environment` from SSM Parameter Store or env vars.

## 2. Memory/cost budget (AWS free tier; full config)

| Component | Lambda `MemorySize` | Notes |
|---|---|---|
| subscriber-registry | 512 MB (SnapStart) | user-facing; SnapStart reduces cold start |
| flood-ingest | 512 MB | EventBridge-triggered; no SnapStart needed |
| rules-engine | 256 MB | SQS-triggered; three separate Lambda functions (flood/heat/aq) |
| alert-fanout (API + planner) | 512 MB (SnapStart) | user-facing broadcast API |
| alert-fanout (channel workers) | 256 MB each | separate Lambda per channel (telegram/web-push/email/mock) |
| cap-ingest | 256 MB | EventBridge-triggered |
| report-intake | 256 MB | |
| signal-fusion | 256 MB | |
| heat-ingest | 256 MB | |
| aq-ingest | 256 MB | |
| alert-composer | 256 MB | |
| audit-evaluation | 256 MB | |
| outbox-relay | 128 MB | EventBridge-triggered every 30 s |

AWS Lambda free tier: **1M requests/month + 400,000 GB-seconds/month** (`⚠ VERIFY-19`). At 256 MB × 1 s per invocation = 0.000256 GB-s; 400,000 / 0.000256 ≈ 1.56B invocations free at 256 MB/1s. Well within free tier for Kestrel's load.
**If approaching limits:** merge audit-evaluation + alert-composer into one function; merge per-channel workers.

## 3. Lambda packaging (multi-module)
- Build stage: `./mvnw -pl services/<svc> -am -DskipTests package` → produces a fat JAR via `spring-boot-maven-plugin` (layered).
- For Lambda: use `aws-lambda-java-runtime-interface-client` + Spring Boot Lambda adapter (or `spring-cloud-function` Lambda adapter).
- Handler class: `com.kestrel.<service>.LambdaHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse>` (for HTTP triggers) or `RequestHandler<SQSEvent, Void>` (for SQS triggers) or `RequestHandler<ScheduledEvent, Void>` (for EventBridge triggers).
- Upload to S3 (SAM/CDK bucket) via `sam build && sam package`.
- Images option: `docker buildx build --platform linux/arm64` + ECR (if image-based Lambda preferred). Script: `infra/scripts/build-images.sh`.

## 4. DynamoDB init
All DynamoDB tables are declared in `infra/sam-template.yaml` (or CDK). Each table:
- `BillingMode: PAY_PER_REQUEST` (on-demand, free tier 25 GB + 25 RCU/WCU provisioned — use provisioned for cost predictability `⚠ VERIFY-20`).
- `PointInTimeRecoveryEnabled: true` (no extra cost for the first 25 GB).
- TTL attribute: `ttl` (enabled on all tables that use expiry for last-good data, outbox, inbox, session, rate-limit).
- Streams: enabled on outbox tables (used by relay Lambda trigger if needed).
- Per-service IAM role with `dynamodb:GetItem, PutItem, UpdateItem, DeleteItem, Query, BatchWrite` only on its own tables. `Condition: {"StringEquals": {"dynamodb:LeadingKeys": ["${aws:PrincipalTag/serviceId}/*"]}}` for row-level isolation where applicable.

## 5. SQS + SNS topology
See `docs/03_data_contracts.md` §2 for full queue/topic list.
- All SQS queues: `MessageRetentionPeriod: 345600` (4 days), `VisibilityTimeout: 60s` (must exceed Lambda timeout), `RedrivePolicy: maxReceiveCount: 3, deadLetterTargetArn: <dlq>`.
- SNS → SQS subscriptions: `RawMessageDelivery: true`; filter policy on `x-event-type` attribute.
- No FIFO queues needed (idempotent consumers tolerate duplicate delivery).
- DLQ monitoring: CloudWatch alarm on `ApproximateNumberOfMessagesVisible > 0` for any DLQ.

## 6. Edge (CloudFront)
- CloudFront distribution with:
  - Origins: one Lambda Function URL per service (no API Gateway, no Caddy).
  - Behaviours: path patterns from `docs/03_data_contracts.md` §5 prefix table → each routes to the corresponding Lambda Function URL origin.
  - Block `/internal/*` paths via CloudFront function (return 403).
  - Cache policy: `CachingDisabled` for all API paths (all dynamic); `CachingOptimized` for the static CAP feed path.
  - Security headers: HSTS, `X-Content-Type-Options`, `Referrer-Policy`, restrictive CSP (via CloudFront response headers policy).
  - Custom domain: `api.<domain>` with ACM certificate (free, auto-renewed).
  - HTTPS only; HTTP redirects to HTTPS.
- Phase 2: add Cognito JWT authorizer as a CloudFront function or Lambda@Edge for JWT validation on coordinator/admin paths.

## 7. Observability (CloudWatch)

### Metrics (mandatory names — published via Micrometer CloudWatch registry)
| Metric | Labels | Producer |
|---|---|---|
| `kestrel.ingest.data_age_seconds` | `feed` | ingest Lambdas |
| `kestrel.feed.state` (0 healthy,1 degraded,2 down) | `feed` | ingest Lambdas |
| `kestrel.external_calls.total` | `feed, outcome` | ingest Lambdas |
| `kestrel.call_budget.used` | `feed, window` | ingest Lambdas |
| `kestrel.signals.published.total` | `hazard` | ingest |
| `kestrel.alert.decisions.total` | `hazard, severity, msgType` | rules-engine |
| `kestrel.rules.eval.seconds` | `hazard` | rules-engine |
| `kestrel.outbox.pending` | — | all |
| `kestrel.fanout.deliveries.total` | `channel, status` | fanout |
| `kestrel.fanout.queue_depth` | `channel` | fanout (from SQS ApproximateNumberOfMessages) |
| `kestrel.fanout.time_to_first_delivery_seconds` / `…to_last…` | `broadcast_or_alert` | fanout |
| `kestrel.fanout.tg_tokens_available` | — | fanout |
| `kestrel.reports.total` | `category, outcome` | report-intake |
| `kestrel.dlq.messages` | `queue` | each (from SQS CloudWatch metric) |

CloudWatch free tier: **10 custom metrics, 10 alarms, 5 GB logs/month** (`⚠ VERIFY-26 equivalent`). Aggregate metrics (e.g. sum per hazard) to stay within 10 custom metrics; use metric math for derived values. CloudWatch Logs: set log retention to 7 days to minimize cost.

### CloudWatch dashboards (provisioned via SAM/CDK)
1. **Feeds**: state, data age per feed, call budget, error rate.
2. **Alerts**: decisions by severity, time from threshold crossing → decision → first send.
3. **Fanout**: SQS queue depth per channel, throughput, failure by code, Telegram bucket level, TTFD/TTLD.
4. **Platform**: Lambda invocations/errors/duration, DynamoDB consumed capacity, SQS message counts, Cognito sign-ins.

CloudWatch Alarms (→ SNS ops notification → Telegram ops chat, optional):
- Feed `DOWN` > 30 min
- DLQ > 0 messages for 10 min
- Outbox pending > 100 for 5 min
- Fanout delivery queue not draining (SQS `NumberOfMessagesSent` = 0 for 30 min while there are visible messages)

## 8. Config & secrets
`.env.example` keys (never commit real values): `DOMAIN, COGNITO_USER_POOL_ID, COGNITO_CLIENT_ID, COGNITO_JWK_URI, ADMIN_BOOTSTRAP_EMAIL/PASSWORD, TELEGRAM_BOT_TOKEN, TELEGRAM_BOT_USERNAME, VAPID_PUBLIC_KEY/PRIVATE_KEY/SUBJECT, OPENAQ_API_KEY, SMTP_*, SACHET_FEED_URLS, OPENMETEO_BASE_URLS, ALLOWED_ORIGINS, AWS_REGION, TABLE_PREFIX, SNS_TOPIC_ARN_SIGNALS, SNS_TOPIC_ARN_ALERTS, SNS_TOPIC_ARN_REGISTRY, SNS_TOPIC_ARN_RECEIPTS`. Store secrets in **AWS Systems Manager Parameter Store** (SecureString, free tier). Lambda IAM role grants `ssm:GetParameter` for its own prefix.

## 9. CI/CD (`.github/workflows/`)
- `backend.yml`: JDK 21, cache Maven, `./mvnw -B verify` (LocalStack via Testcontainers in GitHub Actions), upload JaCoCo.
- `frontend.yml`: Node LTS, `npm ci`, lint, typecheck, test, build, bundle-size budget check (fail > 200 KB gzip initial).
- `deploy.yml` (manual, on tag): `sam build && sam package && sam deploy` (or `cdk deploy`) with AWS credentials from GitHub Secrets. Frontend: Amplify Hosting Git integration (build `npm run build`, output `dist`, SPA fallback `_redirects`: `/* /index.html 200`) for first 12 months; Cloudflare Pages Git integration after month 12 (D-23).
- No Docker build required for Lambda ZIP deployment; optional ECR image push for image-based Lambda.

## 10. Backups & recovery
- `backup.sh`: nightly DynamoDB **on-demand backup** (free for 35 days) + point-in-time recovery always enabled. Export to **S3** (5 GB free tier) weekly. Also export RabbitMQ definitions → **not applicable**; SQS/SNS are managed services with no state to back up.
- `restore.sh` + documented **restore drill** (must be executed once before P4 gate; record time-to-recover). DynamoDB restore from S3 export or PITR.
- Whole stack redeployable from repo + SSM Parameter Store values + latest DynamoDB backup (target < 60 min).

## 11. Mock upstream (`tools/wiremock-upstream`, local dev only)
Open-Meteo-shaped fixtures for: flood (ensemble with scenario files: `calm`, `watch`, `warning`, `severe`, `stale`), history (for Gumbel fit), weather, archive, AQ, OpenAQ, SACHET RSS + CAP XML, Telegram `sendMessage` (configurable 429/403/latency). A `scenario` admin endpoint switches fixtures at runtime so demos/e2e/chaos tests are deterministic. Services switch to mocks via base-URL env vars. LocalStack replaces Docker-based PostgreSQL/Redis/RabbitMQ for local integration testing.

## 12. Tasks acceptance
- `sam deploy --profile dev` → all Lambda functions deploy successfully; DynamoDB/SQS/SNS created; `GET /actuator/health` = UP for all reachable functions.
- Fresh clone + SSM params + one command brings MVP up and passes smoke tests.
- CloudWatch dashboards load with data from a running MVP.
- DynamoDB backup + restore drill passes.
