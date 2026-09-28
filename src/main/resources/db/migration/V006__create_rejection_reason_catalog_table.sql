CREATE TABLE IF NOT EXISTS "sc-acervo-digital".rejection_reason_catalog (
  code VARCHAR(120) PRIMARY KEY,
  target_type VARCHAR(20) NOT NULL,
  description VARCHAR(255) NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
