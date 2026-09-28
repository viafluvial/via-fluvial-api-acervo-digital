package br.com.viafluvial.acervodigital.adapters.out.integration;

import br.com.viafluvial.acervodigital.domain.exception.DomainException;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class VesselOwnershipClient {

    private static final Logger LOG = LoggerFactory.getLogger(VesselOwnershipClient.class);

    private final RestClient restClient;

    public VesselOwnershipClient(
        RestClient.Builder restClientBuilder,
        @Value("${integrations.vessels.base-url:${VESSELS_BASE_URL:${API_EMBARCACOES_BASE_URL:http://localhost:18006}}}")
        String vesselsBaseUrl
    ) {
        if (vesselsBaseUrl == null || vesselsBaseUrl.isBlank()) {
            throw new IllegalArgumentException("integrations.vessels.base-url must not be blank");
        }

        this.restClient = restClientBuilder.baseUrl(vesselsBaseUrl).build();
    }

    public UUID resolveBoatmanId(UUID vesselId) {
        if (vesselId == null) {
            throw new DomainException("INVALID_ENTITY", "EntityId de embarcacao e obrigatorio.", 400);
        }

        try {
            Map<String, Object> payload = restClient
                .get()
                .uri("/api/v1/vessels/{vesselId}", vesselId)
                .headers(headers -> {
                    copyHeader(headers, HttpHeaders.AUTHORIZATION);
                    copyHeader(headers, "X-Roles");
                    copyHeader(headers, "X-User-Id");
                    copyHeader(headers, "X-Internal-User-Id");
                    copyHeader(headers, "X-Boatman-Id");
                })
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            UUID boatmanId = extractBoatmanId(payload);
            if (boatmanId == null) {
                throw new DomainException(
                    "VESSEL_OWNERSHIP_UNAVAILABLE",
                    "Nao foi possivel identificar o barqueiro da embarcacao informada.",
                    502
                );
            }

            return boatmanId;
        } catch (HttpClientErrorException.NotFound ex) {
            throw new DomainException("VESSEL_NOT_FOUND", "Embarcacao informada nao encontrada.", 404);
        } catch (HttpClientErrorException.Forbidden ex) {
            throw new DomainException("ACERVO_SCOPE_FORBIDDEN", "Barqueiro nao pode acessar embarcacao de outro barqueiro.", 403);
        } catch (HttpClientErrorException.Unauthorized ex) {
            throw new DomainException("UNAUTHORIZED", "Autenticacao obrigatoria para validar ownership da embarcacao.", 401);
        } catch (DomainException ex) {
            throw ex;
        } catch (Exception ex) {
            LOG.warn("Falha ao consultar ownership da embarcacao {}", vesselId, ex);
            throw new DomainException("VESSEL_INTEGRATION_ERROR", "Falha ao validar ownership da embarcacao.", 502);
        }
    }

    @SuppressWarnings("unchecked")
    private UUID extractBoatmanId(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }

        Object topLevelBoatmanId = payload.get("boatmanId");
        UUID topLevel = parseUuid(topLevelBoatmanId);
        if (topLevel != null) {
            return topLevel;
        }

        Object nestedData = payload.get("data");
        if (nestedData instanceof Map<?, ?> nestedMap) {
            Object nestedBoatmanId = ((Map<String, Object>) nestedMap).get("boatmanId");
            return parseUuid(nestedBoatmanId);
        }

        return null;
    }

    private UUID parseUuid(Object value) {
        if (value == null) {
            return null;
        }

        try {
            return UUID.fromString(value.toString().trim());
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private String resolveHeader(String name) {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }

        return attributes.getRequest().getHeader(name);
    }

    @SuppressWarnings("null")
    private void copyHeader(HttpHeaders headers, String name) {
        String value = resolveHeader(name);
        if (value == null) {
            return;
        }

        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return;
        }

        headers.set(name, trimmed);
    }
}
