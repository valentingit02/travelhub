package com.travelhub.reservas.seguridad;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.travelhub.common.web.GlobalExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Seguridad del servicio de reservas (dueno de los datos de los viajeros).
 * <ul>
 *   <li>Contrasenas guardadas con BCrypt (nunca en texto plano).</li>
 *   <li>Sesion sin estado: JWT firmado con HS256 que el frontend manda en {@code Authorization: Bearer}.</li>
 *   <li>Buscar y cotizar siguen siendo libres (otros servicios). Aca solo se protege lo que es de cada viajero.</li>
 *   <li>Las llamadas internas (Saga, pagos) van por RabbitMQ, asi que no pasan por este filtro.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SeguridadConfig {

    public static final String EMISOR = "travelhub-reservas";

    /** Rutas que no requieren sesion. Si llega un token vencido a una de estas, se ignora. */
    static final RequestMatcher PUBLICAS = new OrRequestMatcher(
            new AntPathRequestMatcher("/api/auth/**"),
            new AntPathRequestMatcher("/api/viajeros", HttpMethod.POST.name()),   // alta anterior al login (compatibilidad)
            new AntPathRequestMatcher("/api/reservas/compartidas/**"),             // los amigos pagan con un link, sin cuenta
            new AntPathRequestMatcher("/actuator/**"),
            new AntPathRequestMatcher("/v3/api-docs/**"),
            new AntPathRequestMatcher("/swagger-ui/**"),
            new AntPathRequestMatcher("/swagger-ui.html"),
            new AntPathRequestMatcher("/error"));

    @Bean
    SecretKey jwtKey(@Value("${travelhub.seguridad.jwt-secreto}") String secreto) {
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos 32 caracteres (HS256 usa una clave de 256 bits)");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtKey));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtKey) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtKey).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(EMISOR));   // firma + vencimiento + emisor
        return decoder;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, ObjectMapper mapper) throws Exception {
        AuthenticationEntryPoint sinSesion = (req, res, ex) -> {
            boolean traiaToken = ex instanceof OAuth2AuthenticationException;
            escribir(res, mapper, HttpStatus.UNAUTHORIZED, traiaToken
                    ? "Tu sesión venció o no es válida. Ingresá de nuevo."
                    : "Tenes que ingresar para usar esta funcion.");
        };
        AccessDeniedHandler denegado = (req, res, ex) ->
                escribir(res, mapper, HttpStatus.FORBIDDEN, "No tenés permiso para esta operación.");

        http.csrf(AbstractHttpConfigurer::disable)            // API sin cookies: el token va en un header
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                    .requestMatchers(PUBLICAS).permitAll()
                    .anyRequest().authenticated())
            .oauth2ResourceServer(o -> o
                    .jwt(Customizer.withDefaults())
                    .bearerTokenResolver(resolverQueIgnoraPublicas())
                    .authenticationEntryPoint(sinSesion)
                    .accessDeniedHandler(denegado))
            .exceptionHandling(e -> e.authenticationEntryPoint(sinSesion).accessDeniedHandler(denegado));
        return http.build();
    }

    private static BearerTokenResolver resolverQueIgnoraPublicas() {
        DefaultBearerTokenResolver base = new DefaultBearerTokenResolver();
        return (HttpServletRequest req) -> PUBLICAS.matches(req) ? null : base.resolve(req);
    }

    private static void escribir(HttpServletResponse res, ObjectMapper mapper, HttpStatus status, String mensaje)
            throws IOException {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        mapper.writeValue(res.getOutputStream(), GlobalExceptionHandler.error(status, mensaje));
    }
}
