-- =============================================================
-- outbox_events schema — managed by zeabay-outbox (Flyway)
-- =============================================================
-- When spring.flyway.enabled=true, this migration runs before
-- service migrations (V1, V2, ...). Flyway controls the schema.
-- =============================================================

CREATE TABLE IF NOT EXISTS outbox_events (
    id             BIGINT      PRIMARY KEY,
    event_type     VARCHAR(100) NOT NULL,
    topic          VARCHAR(100) NOT NULL,
    aggregate_type VARCHAR(50)  NOT NULL,
    aggregate_id   BIGINT       NOT NULL,
    payload        TEXT         NOT NULL,
    trace_id       VARCHAR(64),
    status         VARCHAR(10)  NOT NULL DEFAULT 'PENDING'
                       CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED')),
    retry_count    INT          NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    published_at   TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_outbox_events_status
    ON outbox_events (status) WHERE status = 'PENDING';

CREATE INDEX IF NOT EXISTS idx_outbox_events_created_at
    ON outbox_events (created_at);
