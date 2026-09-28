INSERT INTO "sc-acervo-digital".rejection_reason_catalog (code, target_type, description, active)
VALUES
  ('BAIXA_QUALIDADE', 'MEDIA', 'Mídia com baixa qualidade visual', TRUE),
  ('DIREITO_USO_NAO_COMPROVADO', 'MEDIA', 'Direito de uso não comprovado', TRUE),
  ('DOCUMENTO_ILEGIVEL', 'DOCUMENT', 'Documento com leitura prejudicada', TRUE),
  ('DOCUMENTO_INVALIDO', 'DOCUMENT', 'Documento inválido', TRUE)
ON CONFLICT (code) DO NOTHING;
