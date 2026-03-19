-- =============================================================
-- outbox_events schema — managed by zeabay-outbox (Flyway)
-- =============================================================

CREATE TABLE IF NOT EXISTS outbox_events (
    id             BIGINT       PRIMARY KEY,
    event_id       VARCHAR(64)  NOT NULL,
    event_type     VARCHAR(100) NOT NULL,
    topic          VARCHAR(100) NOT NULL,
    aggregate_type VARCHAR(50)  NOT NULL,
    aggregate_id   BIGINT       NOT NULL,
    payload        TEXT         NOT NULL,
    produced_from  VARCHAR(100) NOT NULL,
    trace_id       VARCHAR(64),
    status         VARCHAR(10)  NOT NULL DEFAULT 'PENDING'
                       CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED')),
    retry_count    INT          NOT NULL DEFAULT 0,
    produced_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    published_at   TIMESTAMPTZ
);

-- Primary polling query: WHERE status='PENDING' ORDER BY produced_at ASC
-- Partial + composite covers both filter and sort in one index scan
CREATE UNIQUE INDEX IF NOT EXISTS idx_outbox_events_event_id
    ON outbox_events (event_id);

CREATE INDEX IF NOT EXISTS idx_outbox_events_pending_produced_at
    ON outbox_events (produced_at ASC) WHERE status = 'PENDING';

-- Monitoring / retry dashboards
CREATE INDEX IF NOT EXISTS idx_outbox_events_produced_from
    ON outbox_events (produced_from);
