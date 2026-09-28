INSERT INTO "sc-acervo-digital".document_type_catalog (code, name, scope, active)
VALUES
  ('CNH_NAUTICA', 'CNH Náutica', 'EMBARCACAO', TRUE),
  ('REGISTRO_EMBARCACAO', 'Registro da Embarcação', 'EMBARCACAO', TRUE),
  ('CERTIFICADO_INSPECAO', 'Certificado de Inspeção', 'EMBARCACAO', TRUE),
  ('CERTIFICADO_SEGURANCA', 'Certificado de Segurança', 'EMBARCACAO', TRUE),
  ('LICENCA_AMBIENTAL', 'Licença Ambiental', 'EMBARCACAO', TRUE)
ON CONFLICT (code) DO NOTHING;
