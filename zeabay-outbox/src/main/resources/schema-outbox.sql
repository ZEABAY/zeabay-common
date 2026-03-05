CREATE TABLE IF NOT EXISTS outbox_events (
  id              BIGINT       PRIMARY KEY,
  event_type      VARCHAR(100) NOT NULL,
  topic           VARCHAR(100) NOT NULL,
  aggregate_type  VARCHAR(50)  NOT NULL,
  aggregate_id    BIGINT       NOT NULL,
  payload         TEXT         NOT NULL,   -- JSON
  trace_id        VARCHAR(64),
  status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
  retry_count     INT          NOT NULL DEFAULT 0,
  created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  published_at    TIMESTAMPTZ
);
