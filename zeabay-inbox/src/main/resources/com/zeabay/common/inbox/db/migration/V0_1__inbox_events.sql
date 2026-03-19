-- =============================================================
-- inbox_events schema — managed by zeabay-inbox (Flyway)
-- =============================================================

CREATE TABLE IF NOT EXISTS inbox_events (
    id            BIGINT       PRIMARY KEY,
    event_id      VARCHAR(64)  NOT NULL,
    event_type    VARCHAR(100) NOT NULL,
    produced_from VARCHAR(100) NOT NULL,
    trace_id      VARCHAR(64),
    consumed_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (event_id, produced_from)
);

-- UNIQUE (event_id, produced_from) implicitly creates a composite index,
-- covering the idempotency lookup on INSERT.

-- Monitoring / dashboard queries by service or time window
CREATE INDEX IF NOT EXISTS idx_inbox_events_produced_from
    ON inbox_events (produced_from);

CREATE INDEX IF NOT EXISTS idx_inbox_events_consumed_at
    ON inbox_events (consumed_at DESC);