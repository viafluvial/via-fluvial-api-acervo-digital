CREATE OR REPLACE FUNCTION "sc-acervo-digital".set_updated_at()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_media_item_updated_at ON "sc-acervo-digital".media_item;
CREATE TRIGGER trg_media_item_updated_at
BEFORE UPDATE ON "sc-acervo-digital".media_item
FOR EACH ROW
EXECUTE FUNCTION "sc-acervo-digital".set_updated_at();

DROP TRIGGER IF EXISTS trg_private_document_updated_at ON "sc-acervo-digital".private_document;
CREATE TRIGGER trg_private_document_updated_at
BEFORE UPDATE ON "sc-acervo-digital".private_document
FOR EACH ROW
EXECUTE FUNCTION "sc-acervo-digital".set_updated_at();

DROP TRIGGER IF EXISTS trg_document_type_catalog_updated_at ON "sc-acervo-digital".document_type_catalog;
CREATE TRIGGER trg_document_type_catalog_updated_at
BEFORE UPDATE ON "sc-acervo-digital".document_type_catalog
FOR EACH ROW
EXECUTE FUNCTION "sc-acervo-digital".set_updated_at();

DROP TRIGGER IF EXISTS trg_rejection_reason_catalog_updated_at ON "sc-acervo-digital".rejection_reason_catalog;
CREATE TRIGGER trg_rejection_reason_catalog_updated_at
BEFORE UPDATE ON "sc-acervo-digital".rejection_reason_catalog
FOR EACH ROW
EXECUTE FUNCTION "sc-acervo-digital".set_updated_at();
