package com.travelhub.reservas.seguridad;

import com.travelhub.reservas.viajero.Viajero;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/** Emite el JWT de la sesion. El "sub" es el id del viajero; no lleva datos sensibles. */
@Service
public class TokenService {

    public record Emitido(String token, Instant expiraEn) { }

    private final JwtEncoder encoder;
    private final Duration duracion;

    public TokenService(JwtEncoder encoder, @Value("${travelhub.seguridad.jwt-duracion:8h}") Duration duracion) {
        this.encoder = encoder;
        this.duracion = duracion;
    }

    public Emitido emitir(Viajero v) {
        Instant ahora = Instant.now();
        Instant vence = ahora.plus(duracion);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(SeguridadConfig.EMISOR)
                .issuedAt(ahora)
                .expiresAt(vence)
                .subject(String.valueOf(v.getId()))
                .claim("email", v.getEmail())
                .claim("nombre", v.getNombre())
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return new Emitido(token, vence);
    }
}
