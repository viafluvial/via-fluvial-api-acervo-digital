package br.com.viafluvial.acervodigital;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(exclude = OAuth2ResourceServerAutoConfiguration.class)
@ConfigurationPropertiesScan
public class AcervoDigitalApplication {

    public static void main(String[] args) {
        SpringApplication.run(AcervoDigitalApplication.class, args);
    }
}
