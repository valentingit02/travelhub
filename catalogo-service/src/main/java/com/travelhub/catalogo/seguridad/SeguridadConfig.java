package com.travelhub.catalogo.seguridad;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelhub.common.web.GlobalExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * El catalogo no tiene usuarios propios: valida el JWT que emite reservas-service (misma clave JWT_SECRET)
 * y exige el rol ADMIN solo para las operaciones de operador. Buscar, verificar ofertas y el tipo de cambio
 * siguen siendo publicos.
 *
 * Reservar y cancelar en el proveedor (/api/catalogo/reservas-proveedor) lo usa solo reservas-service desde la
 * red interna de Docker; el gateway lo bloquea para el exterior.
 */
@Configuration
@EnableWebSecurity
public class SeguridadConfig {

    static final String EMISOR = "travelhub-reservas";

    /** Lo que solo puede hacer un operador: cambiar precios e inventario y aprobar lo que bloqueo la IA. */
    static final RequestMatcher SOLO_ADMIN = new OrRequestMatcher(
            new AntPathRequestMatcher("/api/catalogo/autos", HttpMethod.POST.name()),
            new AntPathRequestMatcher("/api/catalogo/autos/**", HttpMethod.POST.name()),
            new AntPathRequestMatcher("/api/catalogo/autos/**", HttpMethod.PUT.name()),
            new AntPathRequestMatcher("/api/catalogo/autos/**", HttpMethod.DELETE.name()),
            new AntPathRequestMatcher("/api/catalogo/ofertas/aprobar", HttpMethod.POST.name()),
            new AntPathRequestMatcher("/api/catalogo/ofertas/bloqueadas", HttpMethod.GET.name()));

    @Bean
    JwtDecoder jwtDecoder(@Value("${travelhub.seguridad.jwt-secreto}") String secreto) {
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos 32 caracteres (HS256 usa una clave de 256 bits)");
        }
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(new SecretKeySpec(bytes, "HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(EMISOR));
        return decoder;
    }

    /** "roles": ["ADMIN"] del token -> autoridad ROLE_ADMIN. */
    static JwtAuthenticationConverter rolesDelToken() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");
        roles.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter conv = new JwtAuthenticationConverter();
        conv.setJwtGrantedAuthoritiesConverter(roles);
        return conv;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, ObjectMapper mapper) throws Exception {
        AuthenticationEntryPoint sinSesion = (req, res, ex) -> escribir(res, mapper, HttpStatus.UNAUTHORIZED,
                ex instanceof OAuth2AuthenticationException
                        ? "Tu sesión venció o no es válida. Ingresá de nuevo."
                        : "Esta operación es solo para operadores: ingresá con una cuenta ADMIN.");
        AccessDeniedHandler denegado = (req, res, ex) ->
                escribir(res, mapper, HttpStatus.FORBIDDEN, "Esta operación requiere el rol ADMIN.");

        http.csrf(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                    .requestMatchers(SOLO_ADMIN).hasRole("ADMIN")
                    .anyRequest().permitAll())
            .oauth2ResourceServer(o -> o
                    .jwt(j -> j.jwtAuthenticationConverter(rolesDelToken()))
                    .bearerTokenResolver(soloEnRutasDeAdmin())
                    .authenticationEntryPoint(sinSesion)
                    .accessDeniedHandler(denegado))
            .exceptionHandling(e -> e.authenticationEntryPoint(sinSesion).accessDeniedHandler(denegado));
        return http.build();
    }

    /**
     * El frontend manda el token en todas las llamadas. En las rutas publicas (buscar) se ignora: asi un token
     * vencido nunca rompe una busqueda.
     */
    private static BearerTokenResolver soloEnRutasDeAdmin() {
        DefaultBearerTokenResolver base = new DefaultBearerTokenResolver();
        return (HttpServletRequest req) -> SOLO_ADMIN.matches(req) ? base.resolve(req) : null;
    }

    private static void escribir(HttpServletResponse res, ObjectMapper mapper, HttpStatus status, String mensaje)
            throws IOException {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        mapper.writeValue(res.getOutputStream(), GlobalExceptionHandler.error(status, mensaje));
    }
}
