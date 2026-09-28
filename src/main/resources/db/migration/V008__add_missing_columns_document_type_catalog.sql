ALTER TABLE IF EXISTS "sc-acervo-digital".document_type_catalog
    ADD COLUMN IF NOT EXISTS requires_expiration BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS allows_commercial_use BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS allows_social_media_use BOOLEAN NOT NULL DEFAULT FALSE;
