
### 📘 Swagger API Documentation
➡️ **https://cyber-web-crawler.onrender.com/swagger-ui/index.html#/**

---

### 🔗 API Base URL
➡️ **https://cyber-web-crawler.onrender.com/**

---

# SupplyTrace

> **Website Supply-Chain Integrity Monitor** — crawl, snapshot, diff, score, and alert on third-party dependency changes across any number of sites.

---

## What Is This?

SupplyTrace is a Spring Boot backend that answers one question: **"Has anything in my website's third-party supply chain changed since the last time I checked?"**

It does this by crawling registered URLs, extracting every external dependency (scripts, stylesheets, iframes, plugins, and domains), hashing the result as a snapshot, and then diffing consecutive snapshots to surface additions, removals, and suspicious patterns — all scored on a 0–100 risk scale with per-finding alerts.

---

## What Makes It Unique?

Most website monitoring tools check uptime or performance. SupplyTrace watches the **supply chain** — the same attack surface exploited by Magecart-style skimmer attacks, where a malicious `<script>` tag is quietly injected and steals payment data or credentials.

Three design choices set it apart:

### 1. Poly-Site Relationship Model
The core data model is intentionally **one-to-many-to-many**:

```
MonitoredSite  (1)
    └─── SiteSnapshot  (many, ordered by time)
              └─── SiteSnapshotPayload  (scripts, stylesheets, iframes, plugins, domains)
    └─── Alert  (many, linked to the site)
```

A single `MonitoredSite` accumulates an **unlimited timeline of snapshots**, each independently hashed. This means you can:
- Compare any two points in time (not just last vs. now)
- Track a site's risk score as it evolves over weeks
- See the exact scan where a suspicious domain first appeared
- Run the `GET /sites/{id}/snapshot/compare` endpoint to get a side-by-side diff of the two most recent snapshots

The `SiteSnapshot` entity uses a `@ManyToOne` relation back to `MonitoredSite`, with a composite index on `(site_id, scanned_at)` for fast time-ordered lookups. This is intentionally not a simple "last scan wins" model — every snapshot is a first-class record with its own SHA-256 hash.

### 2. Contextual Risk Scoring (Not Just Presence Detection)
Most scanners flag anything unknown. SupplyTrace applies a **trust-aware scoring engine** that distinguishes between:

| Scenario | Example | Severity |
|---|---|---|
| New script from a trusted CDN | `cdn.jsdelivr.net` added | LOW (score: 2) |
| New script from an unknown domain | `cdn.evil-host.io` added | HIGH (score: 18) |
| Raw IP resource | `http://45.33.32.156/track.js` | HIGH (score: 25) |
| HTTP resource on HTTPS site | Mixed content | HIGH (score: 20) |
| Script removed from trusted domain | Routine deploy | LOW (score: 8) |
| More than 5 untrusted third-party domains | Dependency bloat + risk | MEDIUM (score: 12) |

The `RiskScoringService` maintains a curated allowlist of 60+ trusted domains (Google, Cloudflare, AWS, Stripe, Facebook, GitHub, etc.) and adjusts both `severity` and `scoreContribution` dynamically based on whether a resource host is in that list. A site's cumulative score is capped at 100.

### 3. Snapshot Diff as a First-Class API
The diff between two consecutive snapshots is not just logged — it is:
- Returned in the `POST /sites/{id}/scan` response as a structured `diff` object
- Persisted and retrievable via `GET /sites/{id}/snapshot/compare`
- The basis for **alert generation** (one alert per finding, not per scan)
- Surfaced in the enriched dashboard as `recentChanges` counts per site

The `SnapshotDiffEngine` computes set-difference (`LinkedHashSet` for order stability) across all five resource categories: scripts, stylesheets, iframes, plugins, and external domains. The diff for an **initial scan** is treated specially — all resources are marked as "new" but the `initialScan: true` flag prevents false HIGH alerts on first registration.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Runtime | Java 21 |
| Framework | Spring Boot 3.3.5 |
| Persistence | Spring Data JPA + Hibernate |
| Database | PostgreSQL (prod/dev) · H2 in-memory (local) |
| HTTP crawling | OkHttp3 |
| HTML parsing | Jsoup 1.18.1 |
| API docs | SpringDoc OpenAPI / Swagger UI |
| Boilerplate reduction | Lombok |
| Build | Maven Wrapper (`mvnw`) |

---

## Project Structure

```
src/main/java/com/supply/supplyTrace/
├── site/           # MonitoredSite entity, CRUD, ScanResponse, Dashboard
├── snapshot/       # SiteSnapshot entity & repository, payload record
├── crawler/        # SiteCrawlerService (OkHttp + Jsoup extraction)
├── analyzer/       # SnapshotDiffEngine + RiskScoringService
├── alert/          # Alert entity, AlertService, severity/type enums
├── scheduler/      # SiteScanScheduler (24h cron)
├── config/         # HttpClient, Scheduling, OpenAPI, GlobalExceptionHandler
└── util/           # UrlUtils (trusted-domain allowlist), HashUtils (SHA-256)
```

---

## Quick Start (No Database Setup)

Uses an in-memory H2 database via the `local` profile (default):

```bash
./mvnw spring-boot:run
```

- **API base:** `http://localhost:8080`
- **Swagger UI:** `http://localhost:8080/swagger-ui`
- **Health check:** `http://localhost:8080/actuator/health`

---

## PostgreSQL / Supabase (Dev Profile)

```bash
cp src/main/resources/application-dev.properties.example \
   src/main/resources/application-dev.properties

export SUPPLYTRACE_DB_URL='jdbc:postgresql://YOUR-HOST:5432/YOUR_DB?sslmode=require'
export SUPPLYTRACE_DB_USERNAME='YOUR_USERNAME'
export SUPPLYTRACE_DB_PASSWORD='YOUR_PASSWORD'

./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

`application-dev.properties` is gitignored — no secrets are committed.

---

## Production

```bash
export SPRING_PROFILES_ACTIVE=prod
export SUPPLYTRACE_DB_URL='jdbc:postgresql://...'
export SUPPLYTRACE_DB_USERNAME='...'
export SUPPLYTRACE_DB_PASSWORD='...'
./mvnw spring-boot:run
```

Optional environment variables:

| Variable | Default | Description |
|---|---|---|
| `SUPPLYTRACE_USER_AGENT` | `SupplyTraceBot/1.0` | Crawler user-agent string |
| `SUPPLYTRACE_TIMEOUT_MS` | `12000` | HTTP request timeout |
| `SUPPLYTRACE_SCAN_DELAY_MS` | `86400000` | Delay between scheduled scans (24h) |
| `SUPPLYTRACE_SCAN_INITIAL_DELAY_MS` | `60000` | Initial delay before first auto-scan (1 min) |

Swagger UI is disabled in the `prod` profile.

---

## Build & Test

Requires a full **JDK 21** (not JRE-only). If your default `java` is a newer JRE, set `JAVA_HOME` to a JDK 21 install first.

```bash
./mvnw clean test
```

Test coverage includes:
- `SnapshotDiffEngineTest` — diff logic for adds/removes across all resource types
- `RiskScoringServiceTest` — scoring scenarios (trusted vs. untrusted, mixed content, IP resources)
- `SiteCrawlerServiceTest` — HTML extraction with mock responses
- `UrlUtilsTest` — normalization, host extraction, same-party detection

---

## API Reference

| Method | Path | Description |
|---|---|---|
| `POST` | `/sites` | Register a new site for monitoring |
| `GET` | `/sites` | List all monitored sites |
| `GET` | `/sites/{id}` | Get a single site by ID |
| `DELETE` | `/sites/{id}` | Remove site (cascades snapshots + alerts) |
| `POST` | `/sites/{id}/scan` | Trigger an immediate scan |
| `GET` | `/sites/{id}/alerts` | Get all alerts for a site (newest first) |
| `GET` | `/sites/{id}/snapshot/compare` | Compare the two most recent snapshots |
| `GET` | `/dashboard` | Summary metrics across all sites |
| `GET` | `/dashboard/enriched` | Per-site metrics with change counts and domain stats |

Full interactive docs available at `http://localhost:8080/swagger-ui` when running locally.

---

## Postman Collection (mp 2)

A ready-to-use Postman collection (`mp 2.postman_collection.json`) is included in the project root. Import it directly into Postman to test all endpoints against `localhost:8080`.

### Requests in the collection

#### Add Site
`POST http://localhost:8080/sites`

Register a URL for monitoring. Duplicate URLs are idempotent — re-registering an existing URL returns the existing site record.

```json
{ "url": "https://www.github.com/" }
```

#### Run Scan
`POST http://localhost:8080/sites/1/scan`

Crawls the site, stores a snapshot, diffs against the previous snapshot, computes a risk score, and returns all created alerts. The response shown in the screenshots demonstrates a scan of `github.com` with a `riskScore: 18` and a diff containing newly detected `wp-runtime` and `primer-react` script bundles.

Sample response shape:
```json
{
  "siteId": 1,
  "url": "https://www.github.com/",
  "success": true,
  "riskScore": 18,
  "scannedAt": "2026-05-18T17:31:13.725921202Z",
  "snapshotHash": "a29a06f56e3982f57dd5a864c9f2ab33661dfa6e6c22c686a0069ad312702c67",
  "diff": {
    "newScripts": ["https://github.githubassets.com/assets/wp-runtime-481b83e943166c9f.js"],
    "removedScripts": ["https://github.githubassets.com/assets/wp-runtime-bd4f0d8bef044f0a.js"],
    "hasChanges": true,
    "initialScan": false
  },
  "alertsCreated": [...]
}
```

#### Alerts
`GET http://localhost:8080/sites/1/alerts`

Returns all alerts for site 1, ordered newest first. The screenshots show alerts with types `REMOVED_SCRIPT` and `NEW_SCRIPT` at `LOW` severity (because `github.githubassets.com` is a trusted domain in the allowlist).

#### Dashboard
`GET http://localhost:8080/dashboard`

Returns aggregate counts: total sites, total snapshots, high-risk alert count, and the 10 most recent alerts across all sites.

#### List All Sites
`GET http://localhost:8080/sites`

Returns all registered sites with their current risk score and last scan timestamp.

#### Delete Site
`DELETE http://localhost:8080/sites/2`

Removes a site and cascades deletion of all its snapshots and alerts. Returns `204 No Content`.

#### Enriched Dashboard
`GET http://localhost:8080/dashboard/enriched`

Returns per-site breakdown including: snapshot count, alert count, high-risk alert count, unique third-party domain count, unique script count, and a `recentChanges` figure (script + domain adds/removes between the last two scans).

#### Snapshot Comparison
`GET http://localhost:8080/sites/1/snapshot/compare`

Returns a side-by-side view of the two most recent snapshots for site 1, with full resource lists and a diff object showing exactly what changed.

---

## How a Scan Works (End to End)

```
POST /sites/{id}/scan
        │
        ▼
SiteCrawlerService.crawl(url)
  └─ OkHttp fetches the page HTML
  └─ Jsoup extracts:
       • script[src]        → scripts
       • link[rel=stylesheet] → stylesheets
       • iframe[src]        → iframes
       • plugin-path heuristic → plugins
       • all external hostnames → domains
        │
        ▼
SHA-256 hash of snapshot JSON → stored as SiteSnapshot
        │
        ▼
SnapshotDiffEngine.compare(previous, latest)
  └─ Set-difference per resource category
  └─ initialScan flag if no previous snapshot
        │
        ▼
RiskScoringService.assess(url, payload, diff)
  └─ Per-resource: raw IP, mixed content, suspicious domain
  └─ Per-diff: new/removed scripts, domains, iframes, plugins
  └─ Trust-adjusted severity and score per finding
  └─ Capped at 100
        │
        ▼
AlertService.createAlerts(site, findings)
  └─ One Alert row per RiskFinding
        │
        ▼
ScanResponse returned (diff + alerts + riskScore + snapshotHash)
```

---

## Alert Types

| Type | Trigger | Default Severity |
|---|---|---|
| `NEW_SCRIPT` | New `<script src>` detected | HIGH (LOW if trusted) |
| `REMOVED_SCRIPT` | Existing script disappeared | MEDIUM (LOW if trusted) |
| `NEW_STYLESHEET` | New CSS file detected | LOW |
| `REMOVED_STYLESHEET` | CSS file removed | MEDIUM |
| `NEW_IFRAME` | New iframe detected | MEDIUM |
| `REMOVED_IFRAME` | Iframe removed | MEDIUM |
| `NEW_PLUGIN` | New plugin-path resource | MEDIUM |
| `REMOVED_PLUGIN` | Plugin-path resource removed | MEDIUM |
| `NEW_DOMAIN` | New external domain contacted | HIGH (LOW if trusted) |
| `REMOVED_DOMAIN` | External domain no longer contacted | MEDIUM |
| `RAW_IP_RESOURCE` | Script/stylesheet loaded from a raw IP | HIGH |
| `HTTP_MIXED_CONTENT` | HTTP resource on an HTTPS site | HIGH |
| `SUSPICIOUS_DOMAIN` | Unknown third-party domain in resources | HIGH |
| `TOO_MANY_THIRD_PARTY_DOMAINS` | More than 5 untrusted external domains | MEDIUM |
| `SITE_UNREACHABLE` | Crawl failed (network error, 5xx, etc.) | HIGH |
| `RISK_SCORE_HIGH` | Aggregate score exceeds threshold | HIGH |

---

## Real-World Use Case Demonstrated in the Screenshots

The Postman screenshots in the project show a real scan of `https://www.github.com/`:

- **Run scan response** shows `riskScore: 18` and a diff where two `wp-runtime` and `primer-react` script bundles changed (GitHub's bundled JS assets are content-hashed on each deploy, so their filenames rotate on every release).
- **Alerts response** shows this generated four alerts: two `REMOVED_SCRIPT` and two `NEW_SCRIPT`, all at `LOW` severity because `github.githubassets.com` is in the trusted-domain allowlist — correctly distinguishing a routine deploy from a genuine supply-chain injection.

This is the intended behavior: a clean deployment on a known CDN generates low-noise alerts, while the same change from an unknown domain would generate HIGH alerts and a much higher risk score.

---

## License

This project is provided as-is for educational and evaluation purposes.
