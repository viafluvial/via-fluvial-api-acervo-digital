package br.com.viafluvial.acervodigital.application.media;

import br.com.viafluvial.acervodigital.adapters.in.web.generated.model.AcervoFileResponse;

public record ImageDerivative(
    AcervoFileResponse.VariantEnum variant,
    byte[] content,
    String mimeType,
    int width,
    int height,
    long sizeBytes,
    String checksum
) {}
