# SupplyTrace

SupplyTrace is a lightweight backend for monitoring website supply-chain integrity. It crawls sites, extracts third-party dependencies, stores snapshots, compares changes over time, scores risk, and generates alerts when suspicious drift appears.

## Tech stack

- Java 21
- Spring Boot 3
- Spring Web, Data JPA, Validation, Actuator
- PostgreSQL (production) or H2 (local quick start)
- Jsoup, OkHttp, Lombok
- OpenAPI / Swagger UI (springdoc)

## Features

- Register monitored sites
- Crawl and snapshot external scripts, stylesheets, iframes, plugins, and third-party domains
- Compare snapshots to detect dependency drift
- Calculate a numeric risk score
- Generate alerts for suspicious changes
- Scheduled rescans every 24 hours
- Dashboard summary endpoints

## Quick start (no database setup)

Uses an in-memory H2 database via the `local` profile (default):

```bash
./mvnw spring-boot:run
```

- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui
- Health: http://localhost:8080/actuator/health

## PostgreSQL / Supabase (dev profile)

1. Copy the example config and set credentials via environment variables (recommended) or edit the file:

```bash
cp src/main/resources/application-dev.properties.example src/main/resources/application-dev.properties
export SUPPLYTRACE_DB_URL='jdbc:postgresql://YOUR-HOST:5432/YOUR_DB?sslmode=require'
export SUPPLYTRACE_DB_USERNAME='YOUR_USERNAME'
export SUPPLYTRACE_DB_PASSWORD='YOUR_PASSWORD'
```

2. Run with the `dev` profile:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

`application-dev.properties` is gitignored so secrets are not committed.

## Production

```bash
export SPRING_PROFILES_ACTIVE=prod
export SUPPLYTRACE_DB_URL='jdbc:postgresql://...'
export SUPPLYTRACE_DB_USERNAME='...'
export SUPPLYTRACE_DB_PASSWORD='...'
./mvnw spring-boot:run
```

Optional tuning:

```bash
export SUPPLYTRACE_USER_AGENT='SupplyTraceBot/1.0'
export SUPPLYTRACE_TIMEOUT_MS=12000
export SUPPLYTRACE_SCAN_DELAY_MS=86400000
export SUPPLYTRACE_SCAN_INITIAL_DELAY_MS=60000
```

Swagger is disabled in the `prod` profile.

## Build & test

Requires a **JDK 21** (full JDK, not JRE-only). If your default `java` is a newer JRE without `javac`, set `JAVA_HOME` to a JDK 21 install.

```bash
./mvnw clean test
```

## API endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/sites` | Register a site |
| GET | `/sites` | List sites |
| GET | `/sites/{id}` | Get site |
| DELETE | `/sites/{id}` | Remove site |
| POST | `/sites/{id}/scan` | Run a scan |
| GET | `/sites/{id}/alerts` | Site alerts |
| GET | `/sites/{id}/snapshot/compare` | Compare latest snapshots |
| GET | `/dashboard` | Summary dashboard |
| GET | `/dashboard/enriched` | Detailed dashboard |

## Example request

```bash
curl -X POST http://localhost:8080/sites \
  -H 'Content-Type: application/json' \
  -d '{"url":"https://example.com"}'
```

Interactive docs: http://localhost:8080/swagger-ui
