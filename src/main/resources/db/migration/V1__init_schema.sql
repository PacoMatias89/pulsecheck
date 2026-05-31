-- PulseCheck Database Schema
-- V1: Initial schema

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================
-- USERS
-- ============================================================
CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(255) NOT NULL,
    plan          VARCHAR(50)  NOT NULL DEFAULT 'FREE',
    api_key       VARCHAR(64)  UNIQUE,
    email_verified BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_api_key ON users(api_key);

-- ============================================================
-- WORKSPACES
-- ============================================================
CREATE TABLE workspaces (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id   UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name       VARCHAR(255) NOT NULL,
    slug       VARCHAR(100) UNIQUE NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_workspaces_owner ON workspaces(owner_id);
CREATE INDEX idx_workspaces_slug  ON workspaces(slug);

-- ============================================================
-- MONITORS
-- ============================================================
CREATE TABLE monitors (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id          UUID         NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    name                  VARCHAR(255) NOT NULL,
    url                   TEXT         NOT NULL,
    type                  VARCHAR(50)  NOT NULL DEFAULT 'HTTP',
    method                VARCHAR(10)  NOT NULL DEFAULT 'GET',
    expected_status_code  INT          NOT NULL DEFAULT 200,
    keyword_contains      TEXT,
    request_headers       JSONB        NOT NULL DEFAULT '{}',
    interval_seconds      INT          NOT NULL DEFAULT 300,
    timeout_ms            INT          NOT NULL DEFAULT 30000,
    status                VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    is_active             BOOLEAN      NOT NULL DEFAULT TRUE,
    consecutive_failures  INT          NOT NULL DEFAULT 0,
    last_checked_at       TIMESTAMPTZ,
    last_status_change_at TIMESTAMPTZ,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_monitors_workspace    ON monitors(workspace_id);
CREATE INDEX idx_monitors_status       ON monitors(status);
CREATE INDEX idx_monitors_active       ON monitors(is_active);
CREATE INDEX idx_monitors_last_checked ON monitors(last_checked_at);

-- ============================================================
-- MONITOR CHECKS (time-series data)
-- ============================================================
CREATE TABLE monitor_checks (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    monitor_id      UUID        NOT NULL REFERENCES monitors(id) ON DELETE CASCADE,
    status          VARCHAR(50) NOT NULL,
    response_time_ms BIGINT,
    status_code     INT,
    error_message   TEXT,
    region          VARCHAR(50) NOT NULL DEFAULT 'us-east-1',
    checked_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_checks_monitor_id  ON monitor_checks(monitor_id);
CREATE INDEX idx_checks_checked_at  ON monitor_checks(checked_at DESC);
CREATE INDEX idx_checks_monitor_time ON monitor_checks(monitor_id, checked_at DESC);

-- Auto-partition hint: keep last 90 days for FREE plan, 1 year for PRO+
-- Cleanup job should run weekly

-- ============================================================
-- INCIDENTS
-- ============================================================
CREATE TABLE incidents (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    monitor_id       UUID         NOT NULL REFERENCES monitors(id) ON DELETE CASCADE,
    workspace_id     UUID         NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    title            VARCHAR(500),
    status           VARCHAR(50)  NOT NULL DEFAULT 'OPEN',
    started_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    resolved_at      TIMESTAMPTZ,
    duration_seconds BIGINT,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_incidents_monitor   ON incidents(monitor_id);
CREATE INDEX idx_incidents_workspace ON incidents(workspace_id);
CREATE INDEX idx_incidents_status    ON incidents(status);
CREATE INDEX idx_incidents_started   ON incidents(started_at DESC);

-- ============================================================
-- STATUS PAGES
-- ============================================================
CREATE TABLE status_pages (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id   UUID         NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    name           VARCHAR(255) NOT NULL,
    slug           VARCHAR(100) UNIQUE NOT NULL,
    custom_domain  VARCHAR(255),
    description    TEXT,
    is_public      BOOLEAN      NOT NULL DEFAULT TRUE,
    logo_url       TEXT,
    primary_color  VARCHAR(7)   NOT NULL DEFAULT '#6366f1',
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_status_pages_workspace ON status_pages(workspace_id);
CREATE INDEX idx_status_pages_slug      ON status_pages(slug);

-- ============================================================
-- STATUS PAGE MONITORS
-- ============================================================
CREATE TABLE status_page_monitors (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    status_page_id UUID         NOT NULL REFERENCES status_pages(id) ON DELETE CASCADE,
    monitor_id     UUID         NOT NULL REFERENCES monitors(id) ON DELETE CASCADE,
    display_name   VARCHAR(255),
    order_index    INT          NOT NULL DEFAULT 0,
    UNIQUE(status_page_id, monitor_id)
);

CREATE INDEX idx_spm_page    ON status_page_monitors(status_page_id);
CREATE INDEX idx_spm_monitor ON status_page_monitors(monitor_id);

-- ============================================================
-- NOTIFICATION CHANNELS
-- ============================================================
CREATE TABLE notification_channels (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID         NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    name         VARCHAR(255) NOT NULL,
    type         VARCHAR(50)  NOT NULL,
    config       JSONB        NOT NULL DEFAULT '{}',
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notif_channels_workspace ON notification_channels(workspace_id);

-- ============================================================
-- NOTIFICATION RULES
-- ============================================================
CREATE TABLE notification_rules (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    monitor_id         UUID    NOT NULL REFERENCES monitors(id) ON DELETE CASCADE,
    channel_id         UUID    NOT NULL REFERENCES notification_channels(id) ON DELETE CASCADE,
    notify_on_down     BOOLEAN NOT NULL DEFAULT TRUE,
    notify_on_recovery BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE(monitor_id, channel_id)
);

CREATE INDEX idx_notif_rules_monitor ON notification_rules(monitor_id);
CREATE INDEX idx_notif_rules_channel ON notification_rules(channel_id);

-- ============================================================
-- UPDATED_AT triggers
-- ============================================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_users_updated_at       BEFORE UPDATE ON users        FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_workspaces_updated_at  BEFORE UPDATE ON workspaces   FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_monitors_updated_at    BEFORE UPDATE ON monitors     FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_status_pages_updated_at BEFORE UPDATE ON status_pages FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
