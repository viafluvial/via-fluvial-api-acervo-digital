INSERT INTO "sc-acervo-digital".private_document (
  id, document_public_key, entity_type, entity_id, document_type_code, document_type_name,
  document_number, issuer, issued_at, expires_at, status, visibility, object_path,
  mime_type, size_bytes
)
VALUES (
  '66fda5ec-f57f-4b41-824a-ceff29f007ba',
  'doc_seed_0001',
  'EMBARCACAO',
  '13a95f37-3121-4393-a77f-a77f253fc1a5',
  'LICENCA_OPERACAO',
  'Licença de Operação',
  'LIC-2026-0001',
  'Autoridade Marítima',
  CURRENT_DATE - INTERVAL '30 days',
  CURRENT_DATE + INTERVAL '335 days',
  'AGUARDANDO_APROVACAO',
  'PRIVADA',
  'private-documents/doc_seed_0001/licenca-operacao.pdf',
  'application/pdf',
  178200
)
ON CONFLICT (document_public_key) DO NOTHING;
