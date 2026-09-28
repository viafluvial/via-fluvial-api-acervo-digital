INSERT INTO "sc-acervo-digital".document_type_catalog (code, name, scope, active)
VALUES
  ('LICENCA_OPERACAO', 'Licença de Operação', 'EMBARCACAO', TRUE),
  ('SEGURO_EMBARCACAO', 'Seguro da Embarcação', 'EMBARCACAO', TRUE),
  ('CONTRATO_SOCIAL', 'Contrato Social', 'BARQUEIRO', TRUE)
ON CONFLICT (code) DO NOTHING;
