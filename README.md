# PulseCheck — Uptime Monitoring & Status Pages

> **Know when your app goes down before your users do.**

Uptime monitoring + beautiful status pages in one tool. Built for indie hackers, SaaS teams, and agencies. Starting at $0.

---

## Table of Contents

1. [Product Decision & Market Research](#1-product-decision--market-research)
2. [Product Definition](#2-product-definition)
3. [Architecture](#3-architecture)
4. [Quick Start](#4-quick-start)
5. [API Reference](#5-api-reference)
6. [Launch Strategy](#6-launch-strategy)
7. [Monetization](#7-monetization)
8. [Development](#8-development)

---

## 1. Product Decision & Market Research

### Pain Points Identified

After analyzing Reddit, Hacker News, G2, Capterra, Product Hunt, and Indie Hackers:

| Pain | Frequency | Market Signal |
|------|-----------|---------------|
| Atlassian StatusPage costs $399/mo for custom domains | Very High | 10+ "alternatives" posts on HN/Reddit monthly |
| Pingdom overpriced, neglected by SolarWinds | High | "Pingdom alternatives 2024" is a top search term |
| UptimeRobot free but unreliable, no status pages | High | Thousands of monthly searches for alternatives |
| Statuspage and monitoring are 2 separate tools | High | Agencies paying for both = $100-500/mo |
| Uptime Kuma is loved but requires self-hosting | High | "Uptime Kuma cloud hosted" searched constantly |

### Competitor Analysis

| Tool | Price | Monitors | Status Page | Fatal Flaw |
|------|-------|----------|-------------|------------|
| Pingdom | $14-$119/mo | Per-monitor pricing | Expensive add-on | Neglected by SolarWinds |
| Atlassian StatusPage | $29-$399/mo | None (separate tool!) | Yes | No monitoring. $399 for custom domain. |
| UptimeRobot | $0-$20/mo | 50 free | No | No status pages. Limited. |
| Better Stack | $0-$60/mo | 10 free | Yes | Pricier at scale |
| Instatus | $15/mo | Separate | Yes | New, limited integrations |

### Decision

Build **PulseCheck**: All-in-one uptime monitoring + status pages. Price 3-5x cheaper than incumbents. Target the gap between free-but-limited (UptimeRobot) and expensive-but-feature-rich (Pingdom + StatusPage).

**Why this wins:**
- Atlassian StatusPage has ZERO built-in monitoring — customers need to buy separately
- The combination product at $19/mo is 10x cheaper than the split-tool stack
- Self-hosted Uptime Kuma proves enormous demand; cloud-hosted version fills the gap
- B2B, recurring revenue, low churn (infrastructure tools are sticky)

---

## 2. Product Definition

**Name:** PulseCheck
**Domain:** pulsecheck.io
**Tagline:** Know before your users do.

### Target Customer

- **Primary:** Indie hackers and SaaS founders with 2-50 services to monitor
- **Secondary:** Digital agencies managing 10-200 client websites

### Core Use Case

1. User adds their API/website URL
2. PulseCheck checks it every 1-5 minutes
3. If it goes down — instant Slack/email alert
4. Users can see a public status page (status.theirapp.com)
5. Incident auto-created, auto-resolved when service recovers

### Feature Matrix

| Feature | Free | Pro ($19) | Agency ($49) | Enterprise |
|---------|------|-----------|--------------|------------|
| Monitors | 5 | 50 | 200 | Unlimited |
| Check interval | 5 min | 1 min | 30 sec | 15 sec |
| Status pages | 1 | 3 | 10 | Unlimited |
| Custom domain | No | No | Yes | Yes |
| Email alerts | Yes | Yes | Yes | Yes |
| Slack/Discord | No | Yes | Yes | Yes |
| Webhook | No | Yes | Yes | Yes |
| Telegram | No | No | Yes | Yes |
| History retention | 90 days | 1 year | Unlimited | Unlimited |
| REST API | Yes | Yes | Yes | Yes |

---

## 3. Architecture

### Stack

- **Runtime:** Java 21
- **Framework:** Spring Boot 3.2
- **Database:** PostgreSQL 16
- **Authentication:** JWT (jjwt 0.12)
- **Migrations:** Flyway
- **Docs:** SpringDoc OpenAPI (Swagger UI)
- **Containers:** Docker + Docker Compose

### Monitor Check Flow

```
Scheduler (every 30s)
    |
    +-- Query: monitors WHERE active=true AND last_checked_at <= now()-interval
    |
    +-- For each monitor (async, thread pool of 20):
            |
            +-- HTTP GET/POST to URL with timeout
            +-- Check status code == expected
            +-- (Optional) Check body contains keyword
            |
            +-- [SUCCESS]
            |     +-- Save check (status=UP)
            |     +-- Reset consecutive_failures = 0
            |     +-- If was DOWN -> resolve incident + send recovery alert
            |
            +-- [FAILURE]
                  +-- Save check (status=DOWN)
                  +-- Increment consecutive_failures
                  +-- If consecutive_failures >= 3:
                        +-- Create incident (if not already open)
                        +-- Send down alerts to all configured channels
```

### Data Model

```
users (1) ------- (N) workspaces (1) ------- (N) monitors
                            |                      |
                            |                      +-- (N) monitor_checks
                            |                      +-- (N) incidents
                            |
                            +-- (N) status_pages --- (N) status_page_monitors
                            +-- (N) notification_channels
                                        |
                                     (N) notification_rules -- (N) monitors
```

---

## 4. Quick Start

### Run with Docker Compose (Recommended)

```bash
git clone https://github.com/pacomatias89/pulsecheck.git
cd pulsecheck
cp .env.example .env
# Edit .env — change passwords and JWT secret at minimum
docker compose up -d
```

- App: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health

### Run Locally (Development)

```bash
# Start only PostgreSQL
docker compose up db -d

# Run Spring Boot app in dev mode
export SPRING_PROFILES_ACTIVE=dev
mvn spring-boot:run
```

### Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `DB_HOST` | Yes | PostgreSQL host |
| `DB_NAME` | Yes | Database name |
| `DB_USER` | Yes | Database user |
| `DB_PASSWORD` | Yes | Database password |
| `JWT_SECRET` | Yes | Min 32-char secret for JWT signing |
| `JWT_EXPIRATION_MS` | No | Token TTL (default: 86400000 = 24h) |
| `MAIL_HOST` | Yes | SMTP host (e.g. smtp.gmail.com) |
| `MAIL_USERNAME` | Yes | SMTP username |
| `MAIL_PASSWORD` | Yes | SMTP password |
| `MAIL_FROM` | No | From address |
| `APP_BASE_URL` | No | Base URL for email links |

---

## 5. API Reference

Full docs at `/swagger-ui.html`. Quick reference:

### Authentication

```
POST /api/auth/register    { email, password, fullName }
POST /api/auth/login       { email, password }
GET  /api/auth/me          (Bearer token required)
```

### Monitors

```
GET    /api/monitors
POST   /api/monitors         { name, url, type, intervalSeconds, ... }
GET    /api/monitors/{id}
PUT    /api/monitors/{id}
DELETE /api/monitors/{id}
POST   /api/monitors/{id}/pause
POST   /api/monitors/{id}/resume
GET    /api/monitors/{id}/checks?limit=100
GET    /api/monitors/{id}/stats
POST   /api/monitors/{id}/notification-rules
```

### Status Pages

```
GET    /api/status-pages
POST   /api/status-pages     { name, slug, description, isPublic, primaryColor }
GET    /api/status-pages/{id}
DELETE /api/status-pages/{id}
POST   /api/status-pages/{id}/monitors
DELETE /api/status-pages/{id}/monitors/{monitorId}
GET    /public/status/{slug}   (no auth — public)
```

### Dashboard

```
GET /api/dashboard/stats    — { totalMonitors, monitorsUp, monitorsDown, openIncidents, overallUptime }
```

### Example: Create Monitor

```bash
# First, register and get token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"you@example.com","password":"yourpassword"}' | jq -r .token)

# Create a monitor
curl -X POST http://localhost:8080/api/monitors \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "My API",
    "url": "https://api.myapp.com/health",
    "type": "HTTPS",
    "intervalSeconds": 60,
    "timeoutMs": 10000
  }'

# Create a status page
curl -X POST http://localhost:8080/api/status-pages \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"My Status Page","slug":"my-app","isPublic":true}'

# Public status page (no auth needed)
curl http://localhost:8080/public/status/my-app
```

---

## 6. Launch Strategy

### Phase 1: Week 1-2 — Community Launch

**Product Hunt:**
- Title: "PulseCheck — Uptime monitoring + status pages for $19/mo (vs $119 Pingdom + $399 StatusPage)"
- Hunter: reach out to indie hacker PH hunters for support
- Launch on Tuesday/Wednesday for best visibility

**Reddit posts:**

r/SaaS:
> "I got tired of paying $119/mo Pingdom + $399/mo StatusPage for 20 services. StatusPage doesn't even DO monitoring — it's purely a communication tool. Built PulseCheck: HTTP monitoring + status pages + incident management in one. Free tier for 5 monitors. Feedback welcome."

r/selfhosted:
> "Love Uptime Kuma but tired of maintaining it? PulseCheck is the hosted version: same simplicity, with built-in status pages and better alerting. Docker Compose if you prefer self-hosting."

r/webdev:
> "Managing 40 client sites. Was paying $300+/month just for status pages. Built an alternative — $49/mo for 200 monitors and 10 status pages. Agencies and freelancers welcome."

**Hacker News:**
> Show HN: PulseCheck — open source uptime monitoring + status pages (Java/Spring Boot)
> Context: Built after paying $119/mo Pingdom + $399/mo StatusPage. StatusPage does ZERO monitoring — completely separate tool. This combines both for $19/mo.

### Phase 2: Week 3-4 — Direct Outreach

**LinkedIn post (for agency segment):**
> Most agencies managing client sites are paying:
> - $119/mo Pingdom for uptime monitoring
> - $299/mo StatusPage to communicate with clients
> Total: $418/month. For 20 sites.
>
> PulseCheck does both for $49/month. 200 monitors. 10 status pages. Slack/Discord/email alerts.
>
> 14-day free trial. No credit card.

**Cold email template:**
> Subject: Cutting your monitoring costs by 90%
>
> Hi [Name],
>
> I noticed [Agency] manages client websites. Quick question: are you using Pingdom + StatusPage?
>
> Most agencies spend $200-500/month on tools that could be replaced by one $49/month product.
>
> PulseCheck monitors 200+ sites, generates per-client status pages, and sends Slack alerts when anything breaks. 14-day free trial.
>
> Worth 5 minutes? Happy to set it up together.

### Phase 3: SEO Foundation

**Target keywords:**
- "statuspage alternative" (2,400 searches/mo)
- "pingdom alternative cheap" (1,600/mo)
- "uptime robot alternative" (1,200/mo)
- "atlassian statuspage too expensive" (400/mo)

**Content:**
1. "Atlassian StatusPage Pricing: Why $399/mo for a Custom Domain is Absurd"
2. "Pingdom vs PulseCheck: Full Comparison 2025"
3. Comparison landing pages at `/alternatives/statuspage`, `/alternatives/pingdom`

---

## 7. Monetization

### Pricing

| Plan | Monthly | Annual (20% off) |
|------|---------|-----------------|
| Free | $0 | $0 |
| Pro | $19/mo | $182/yr |
| Agency | $49/mo | $470/yr |
| Enterprise | $149/mo | $1,430/yr |

### Revenue Projections

**10 customers:**
- Conservative (all Pro): $190/mo
- Realistic (3 Pro + 5 Agency + 2 Enterprise): **$600/mo**

**50 customers:**
- Conservative (all Pro): $950/mo
- Realistic (30 Pro + 15 Agency + 5 Enterprise): **$1,820/mo**

**100 customers:**
- Conservative: $1,900/mo
- Realistic (50 Pro + 35 Agency + 15 Enterprise): **$4,885/mo**

**500 customers:**
- Conservative: $9,500/mo
- Realistic (200 Pro + 200 Agency + 100 Enterprise): **$27,700/mo**

### Unit Economics

- **CAC target:** Under $50 (organic and content-driven)
- **LTV Pro (18-month avg):** $342
- **LTV Agency (24-month avg):** $1,176
- **Gross margin:** ~92% (infrastructure cost ~$1.50/customer/month)
- **Path to $10K MRR:** ~8 months with focused outreach

### Conversion Strategy

- Free to Pro: target 8% (hit plan limits = upgrade prompt)
- Trial to Paid: target 35% (agencies feel immediate pain)
- Annual pre-payment: offer 20% discount (improves cash flow)

---

## 8. Development

### Project Structure

```
src/main/java/com/pulsecheck/
├── PulseCheckApplication.java
├── config/
│   ├── SecurityConfig.java       — JWT + CORS + Spring Security 6
│   └── AsyncConfig.java          — Thread pool for monitor checks
├── domain/
│   ├── entity/                   — JPA entities (User, Monitor, Incident, etc.)
│   └── enums/                    — MonitorStatus, UserPlan, CheckStatus, etc.
├── repository/                   — Spring Data JPA repositories
├── service/
│   ├── AuthService.java          — Registration, login, workspace auto-creation
│   ├── MonitorService.java       — CRUD + plan limit enforcement
│   ├── MonitorCheckService.java  — Check history + uptime stats
│   ├── IncidentService.java      — Incident lifecycle management
│   ├── StatusPageService.java    — Status page CRUD + monitor assignment
│   ├── NotificationService.java  — Multi-channel alert dispatch
│   ├── EmailService.java         — HTML email templates
│   └── WorkspaceService.java     — Workspace isolation
├── scheduler/
│   ├── MonitorScheduler.java     — @Scheduled trigger every 30s
│   └── HttpCheckExecutor.java    — HTTP check + incident logic
├── controller/                   — REST controllers (7 total)
├── dto/
│   ├── request/                  — Validated Java records
│   └── response/                 — Response Java records
├── security/                     — JWT filter, UserDetailsService
└── exception/                    — GlobalExceptionHandler
```

### Running Tests

```bash
mvn test
```

### Building for Production

```bash
mvn clean package -DskipTests
docker build -t pulsecheck:latest .
```

---

## License

MIT — free to use, modify, and self-host.

Built with Java 21 + Spring Boot 3.2 + PostgreSQL 16 + Docker.
