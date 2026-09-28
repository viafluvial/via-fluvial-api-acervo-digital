ALTER TABLE "sc-acervo-digital".media_item
  ADD COLUMN IF NOT EXISTS platform_showcase_enabled BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE "sc-acervo-digital".media_item
SET platform_showcase_enabled = FALSE
WHERE platform_showcase_enabled IS NULL;
