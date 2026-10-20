# 12 — api-gateway (Phase 2) · CloudFront + Cognito + Lambda Function URLs

Tasks **P2-16 … P2-19**. **CloudFront distribution with Cognito JWT authorizer (Lambda@Edge or CloudFront Functions)**; no Spring Cloud Gateway; no dedicated gateway Lambda. `⚠ VERIFY-26`: confirm CloudFront Lambda Function URL integration and custom-domain setup. `⚠ VERIFY-25`: confirm Cognito JWT format for CloudFront authorizer.

## 1. Responsibilities
Single HTTPS entry (CloudFront), path routing to Lambda Function URL origins, Cognito JWT validation (for coordinator/admin paths), CORS, DynamoDB-backed rate limiting, circuit breakers (Lambda concurrency limits + fallback), request-ID propagation, header sanitation. **No business logic, no DB of its own.**

## 2. Route table (CloudFront behaviours)
| Path pattern | Target origin (Lambda Function URL) | Auth |
|---|---|---|
| `/api/v1/auth/**` | subscriber-registry | public (login rate-limited hard) |
| `/api/v1/locations/**` (GET), `/api/v1/push/**` | subscriber-registry | public |
| `/api/v1/subscriptions/**` | subscriber-registry | public + subscriber token (service validates) |
| `/api/v1/coordinators/**`, `/api/v1/subscribers/**` | subscriber-registry | Cognito JWT |
| `/api/v1/flood/public/**` | flood-ingest | public (rate limited) |
| `/api/v1/flood/**` | flood-ingest | Cognito JWT COORDINATOR/ADMIN |
| `/api/v1/status/**`, `/api/v1/rules/**` | rules-engine | Cognito JWT (rules: ADMIN) |
| `/api/v1/broadcasts/**`, `/api/v1/deliveries/**`, `/api/v1/fanout/**`, `/api/v1/alerts/**` | alert-fanout | Cognito JWT |
| `/api/v1/official/**` | cap-ingest | Cognito JWT |
| `POST /api/v1/reports`, `/api/v1/reports/public/**` | report-intake | public (strict rate limit) |
| `/api/v1/reports/**` (others) | report-intake | Cognito JWT |
| `/api/v1/fusion/**` | signal-fusion | Cognito JWT |
| `/api/v1/heat/**`, `/api/v1/aq/**` | heat/aq-ingest | Cognito JWT |
| `/api/v1/cap/**` | alert-composer / S3 (static feed) | public (CAP feed) / Cognito JWT (templates) |
| `/api/v1/audit/**` | audit-evaluation | Cognito JWT |
| `POST /api/v1/feedback` | audit-evaluation | public (rate limited) |
| `/cap/feed.xml`, `/cap/alerts/*.xml` | S3 origin (static CAP files) | public |
| `/internal/**`, `/actuator/**` | — | **403 via CloudFront Function** |

## 3. CloudFront policies / filters
- **Cognito JWT validation**: CloudFront Function (JS, no Lambda@Edge needed for JWT validation at edge) runs on `viewer-request`; checks `Authorization: Bearer <cognito-jwt>` against Cognito JWKS URI (cached in function); for protected paths returns 401/403 if invalid.
- **DynamoDB rate limiting**: DynamoDB-backed counters updated by the downstream Lambda (not at CloudFront level — CloudFront doesn't natively support DynamoDB); rate limit enforcement is in the service Lambda for now. If needed, a lightweight Lambda@Edge for rate limit headers can be added (note: Lambda@Edge has warm-up cost).
- **Circuit breakers**: Lambda concurrency limits act as natural bulkheads. CloudFront returns `503 problem+json` when Lambda throttles (configure custom error response).
- **CORS**: CloudFront response headers policy for configured `ALLOWED_ORIGINS` (`app.<domain>`), `Access-Control-Allow-Credentials: true` for refresh cookie paths.
- `X-Request-Id`: generate in CloudFront Function if absent, propagate downstream.
- Strip `X-Forwarded-For` spoofing beyond the first entry; max request body 64 KB for public routes (CloudFront WAF optional).
- **Aggregated health route** `GET /api/v1/platform/health` (COORDINATOR): a dedicated `kestrel-platform-health` Lambda pings all service `/actuator/health` endpoints, caches 10 s in DynamoDB.

## 4. Tests
Route contract tests with WireMock or LocalStack (every path: auth required/not); 401/403 behaviours; rate-limit trip (DynamoDB counter); `/internal` and `/actuator` blocked (CloudFront Function); CORS preflight; header sanitation; Cognito JWT expiry rejected.

## 5. Task breakdown
P2-16 CloudFront distribution + route table + CORS + VERIFY-25/26 · P2-17 Cognito JWT CloudFront Function + role rules · P2-18 DynamoDB rate limits + circuit breakers (Lambda reserved concurrency) + fallbacks + platform health Lambda · P2-19 CloudFront P2 cut-over + route contract tests + README.
