package com.travelhub.reservas.seguridad;

import com.travelhub.reservas.viajero.Viajero;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Emite el JWT de la sesion. El "sub" es el id del viajero; no lleva datos sensibles.
 * El claim "roles" dice que puede hacer: ["ADMIN"] para los operadores (emails en ADMIN_EMAILS), [] para el resto.
 * catalogo-service valida el mismo token y exige ADMIN para cambiar precios y aprobar productos.
 */
@Service
public class TokenService {

    public static final String ROL_ADMIN = "ADMIN";

    public record Emitido(String token, Instant expiraEn, List<String> roles) { }

    private final JwtEncoder encoder;
    private final Duration duracion;
    private final Set<String> admins;

    @Autowired
    public TokenService(JwtEncoder encoder,
                        @Value("${travelhub.seguridad.jwt-duracion:8h}") Duration duracion,
                        @Value("${travelhub.seguridad.admins:}") List<String> admins) {
        this.encoder = encoder;
        this.duracion = duracion;
        this.admins = admins.stream().map(String::trim).filter(s -> !s.isEmpty())
                .map(String::toLowerCase).collect(Collectors.toUnmodifiableSet());
    }

    public TokenService(JwtEncoder encoder, Duration duracion) {
        this(encoder, duracion, List.of());
    }

    public List<String> roles(Viajero v) {
        return admins.contains(v.getEmail().toLowerCase()) ? List.of(ROL_ADMIN) : List.of();
    }

    public Emitido emitir(Viajero v) {
        Instant ahora = Instant.now();
        Instant vence = ahora.plus(duracion);
        List<String> roles = roles(v);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(SeguridadConfig.EMISOR)
                .issuedAt(ahora)
                .expiresAt(vence)
                .subject(String.valueOf(v.getId()))
                .claim("email", v.getEmail())
                .claim("nombre", v.getNombre())
                .claim("roles", roles)
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return new Emitido(token, vence, roles);
    }
}
