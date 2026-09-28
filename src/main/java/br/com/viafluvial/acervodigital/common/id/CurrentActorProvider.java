package br.com.viafluvial.acervodigital.common.id;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class CurrentActorProvider {

    private static final String BOATMAN_SCOPE_HEADER = "X-Boatman-Id";

    public UUID currentActorId() {
        if (isBoatmanScopedActor()) {
            UUID scopedBoatmanId = currentScopedBoatmanId();
            if (scopedBoatmanId == null) {
                throw new AccessDeniedException("Cabecalho X-Boatman-Id obrigatorio para actor barqueiro");
            }

            return scopedBoatmanId;
        }

        Authentication authentication = currentAuthentication();
        if (authentication != null) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof Jwt jwt) {
                UUID fromJwtSub = parseUuid(jwt.getSubject());
                if (fromJwtSub != null) {
                    return fromJwtSub;
                }
            }

            UUID fromName = parseUuid(authentication.getName());
            if (fromName != null) {
                return fromName;
            }

            if (principal instanceof String principalAsString) {
                UUID fromPrincipal = parseUuid(principalAsString);
                if (fromPrincipal != null) {
                    return fromPrincipal;
                }
            }
        }

        throw new AccessDeniedException("Actor ID ausente ou invalido na autenticacao da requisicao");
    }

    public boolean hasAnyRole(String... roles) {
        Authentication authentication = currentAuthentication();
        if (authentication == null || authentication.getAuthorities() == null || roles == null || roles.length == 0) {
            return false;
        }

        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (authority == null || authority.getAuthority() == null) {
                continue;
            }

            String normalizedAuthority = authority.getAuthority().trim().toUpperCase(Locale.ROOT);
            for (String role : roles) {
                if (role == null || role.isBlank()) {
                    continue;
                }

                String normalizedRole = role.trim().toUpperCase(Locale.ROOT);
                String roleWithPrefix = normalizedRole.startsWith("ROLE_") ? normalizedRole : "ROLE_" + normalizedRole;
                if (normalizedAuthority.equals(roleWithPrefix)) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean isBoatmanScopedActor() {
        return hasAnyRole("BARQUEIRO", "GESTOR", "BOATMAN");
    }

    private Authentication currentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private UUID currentScopedBoatmanId() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }

        String rawBoatmanId = request.getHeader(BOATMAN_SCOPE_HEADER);
        if (rawBoatmanId == null || rawBoatmanId.isBlank()) {
            return null;
        }

        UUID boatmanId = parseUuid(rawBoatmanId);
        if (boatmanId == null) {
            throw new AccessDeniedException("Cabecalho X-Boatman-Id invalido para actor barqueiro");
        }

        return boatmanId;
    }

    private HttpServletRequest currentRequest() {
        var attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }

        return null;
    }

    private UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
