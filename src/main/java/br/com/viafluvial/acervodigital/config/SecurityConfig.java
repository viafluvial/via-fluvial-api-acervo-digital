package br.com.viafluvial.acervodigital.config;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final Map<String, Set<String>> ROLE_ALIASES = Map.ofEntries(
        Map.entry("ROLE_PASSAGEIRO", Set.of("ROLE_USER")),
        Map.entry("ROLE_GESTOR", Set.of("ROLE_USER")),
        Map.entry("ROLE_BARQUEIRO", Set.of("ROLE_USER")),
        Map.entry("ROLE_OPERADOR_AGENCIA", Set.of("ROLE_USER")),
        Map.entry("ROLE_VENDEDOR", Set.of("ROLE_USER")),
        Map.entry("ROLE_SUPORTE", Set.of("ROLE_USER")),
        Map.entry("ROLE_ADMINISTRADOR", Set.of("ROLE_ADMIN")),
        Map.entry("ROLE_AUDITOR", Set.of("ROLE_DPO"))
    );

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            @Value("${security.mode:oauth2}") String securityMode,
            @Value("${security.dev.token:dev-token}") String devToken) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        if ("oauth2".equalsIgnoreCase(securityMode)) {
            http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/swagger-ui/**", "/v3/api-docs/**", "/openapi/**", "/acervo/public/**").permitAll()
                    .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        } else {
            http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/swagger-ui/**", "/v3/api-docs/**", "/openapi/**", "/acervo/public/**").permitAll()
                .anyRequest().authenticated())
                .addFilterBefore(new DevHeaderAuthenticationFilter(devToken), BasicAuthenticationFilter.class);
        }

        return http.build();
    }

    static final class DevHeaderAuthenticationFilter extends OncePerRequestFilter {

        DevHeaderAuthenticationFilter(String devToken) {
            // Token kept for backward-compatible constructor signature.
        }

        @Override
        protected void doFilterInternal(
                @NonNull HttpServletRequest request,
                @NonNull HttpServletResponse response,
                @NonNull FilterChain filterChain) throws ServletException, IOException {
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                Collection<? extends GrantedAuthority> authorities = resolveAuthorities(request);
                String principal = resolvePrincipal(request);
                var authentication = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                    principal,
                    "N/A",
                    authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

            try {
                filterChain.doFilter(request, response);
            } finally {
                SecurityContextHolder.clearContext();
            }
        }

        private Collection<? extends GrantedAuthority> resolveAuthorities(HttpServletRequest request) {
            String rolesHeader = request.getHeader("X-Roles");
            String source = (rolesHeader == null || rolesHeader.isBlank()) ? "ROLE_USER" : rolesHeader;

            List<SimpleGrantedAuthority> roles = Arrays.stream(source.split(","))
                .map(value -> value == null ? "" : value.trim())
                .filter(value -> !value.isBlank())
                .flatMap(value -> expandAuthorities(value).stream())
                .distinct()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

            if (roles.isEmpty()) {
                return List.of(new SimpleGrantedAuthority("ROLE_USER"));
            }

            return roles;
        }

        private Set<String> expandAuthorities(String role) {
            String normalized = normalizeRole(role);
            Set<String> expanded = new HashSet<>();
            expanded.add(normalized);
            expanded.addAll(ROLE_ALIASES.getOrDefault(normalized, Set.of()));
            return expanded;
        }

        private String normalizeRole(String role) {
            String normalized = role.toUpperCase(Locale.ROOT);
            return normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized;
        }

        private String resolvePrincipal(HttpServletRequest request) {
            String userId = request.getHeader("X-User-Id");
            if (userId == null || userId.isBlank()) {
                return "dev-user";
            }
            return userId;
        }
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${security.cors.allowed-origins:http://localhost:8080,http://127.0.0.1:8080,http://localhost:5173,http://127.0.0.1:5173}")
            String allowedOriginsProperty) {
        List<String> allowedOrigins = Arrays.stream(allowedOriginsProperty.split(","))
            .map(value -> value == null ? "" : value.trim())
                .filter(origin -> !origin.isBlank())
                .toList();

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Location", "X-Correlation-Id", "Content-Disposition"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
