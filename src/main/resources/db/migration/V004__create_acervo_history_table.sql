CREATE TABLE IF NOT EXISTS "sc-acervo-digital".acervo_history (
  id UUID PRIMARY KEY,
  item_public_key VARCHAR(80) NOT NULL,
  item_type VARCHAR(20) NOT NULL,
  event_code VARCHAR(100) NOT NULL,
  status_before VARCHAR(30),
  status_after VARCHAR(30),
  reason_code VARCHAR(120),
  reason_description TEXT,
  actor_type VARCHAR(20),
  actor_id VARCHAR(100),
  correlation_id VARCHAR(100),
  created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_acervo_history_item_public_key
  ON "sc-acervo-digital".acervo_history(item_public_key);
