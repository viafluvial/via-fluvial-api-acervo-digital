package br.com.viafluvial.acervodigital.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "acervo")
public record AcervoProperties(Api api, Storage storage) {

    public record Api(String basePath) {}

    public record Storage(
        String provider,
        String quarantineBucket,
        String publicMediaBucket,
        String privateDocumentsBucket,
        String cdnBaseUrl,
        String localBasePath,
        String gcpProjectId,
        String gcpCredentialsFile
    ) {}
}
