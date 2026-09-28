package br.com.viafluvial.acervodigital.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security")
public record SecurityModeProperties(String mode, OAuth2 oauth2) {

    public record OAuth2(String issuerUri, String jwksUri) {}
}
