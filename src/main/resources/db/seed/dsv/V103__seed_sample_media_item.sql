INSERT INTO "sc-acervo-digital".media_item (
  id, media_public_key, entity_type, entity_id, owner_public_key, boat_public_key,
  accommodation_code, media_type, purpose, visibility, status, title, description,
  is_main, display_order, object_path, mime_type, size_bytes, source_type,
  responsible_declaration, has_image_use_authorization, allows_commercial_use,
  allows_marketing_use, allows_social_media_use
)
VALUES (
  'b079e0da-5f8d-4706-bd26-10a06776f760',
  'med_seed_0001',
  'EMBARCACAO',
  '13a95f37-3121-4393-a77f-a77f253fc1a5',
  'owner_seed_0001',
  'boat_seed_0001',
  'camarote',
  'IMAGE',
  'GALERIA',
  'PUBLICA',
  'AGUARDANDO_APROVACAO',
  'Foto de cabine',
  'Mídia sintética para ambiente dsv',
  FALSE,
  0,
  'quarantine/med_seed_0001/foto-cabine.jpg',
  'image/jpeg',
  204800,
  'ENVIADA_PELO_BARQUEIRO',
  TRUE,
  TRUE,
  TRUE,
  FALSE,
  FALSE
)
ON CONFLICT (media_public_key) DO NOTHING;
