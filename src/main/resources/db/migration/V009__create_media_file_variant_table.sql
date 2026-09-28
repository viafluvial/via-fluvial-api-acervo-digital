CREATE TABLE IF NOT EXISTS "sc-acervo-digital".media_file_variant (
  id UUID PRIMARY KEY,
  media_id UUID NOT NULL REFERENCES "sc-acervo-digital".media_item(id) ON DELETE CASCADE,
  variant VARCHAR(40) NOT NULL,
  bucket_type VARCHAR(30) NOT NULL,
  object_path TEXT NOT NULL,
  mime_type VARCHAR(150) NOT NULL,
  size_bytes BIGINT NOT NULL,
  width INTEGER,
  height INTEGER,
  duration_seconds INTEGER,
  checksum VARCHAR(255),
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  CONSTRAINT uk_media_file_variant_media_variant UNIQUE (media_id, variant)
);

CREATE INDEX IF NOT EXISTS idx_media_file_variant_media_id
  ON "sc-acervo-digital".media_file_variant(media_id);
