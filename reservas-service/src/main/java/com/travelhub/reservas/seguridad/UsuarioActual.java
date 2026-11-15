package com.travelhub.reservas.seguridad;

import com.travelhub.common.web.ForbiddenException;
import com.travelhub.common.web.UnauthorizedException;
import org.springframework.security.oauth2.jwt.Jwt;

/** Quien hace el request, segun el token. Cada viajero solo ve y toca lo suyo. */
public final class UsuarioActual {

    private UsuarioActual() { }

    public static Long id(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) {
            throw new UnauthorizedException("Tenes que ingresar para usar esta funcion.");
        }
        try {
            return Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException e) {
            throw new UnauthorizedException("Tu sesion no es valida. Ingresa de nuevo.");
        }
    }

    /** Si el request trae un viajeroId tiene que ser el del token; si no trae, se usa el del token. */
    public static Long exigirMismo(Jwt jwt, Long viajeroPedido) {
        Long id = id(jwt);
        if (viajeroPedido != null && !viajeroPedido.equals(id)) {
            throw new ForbiddenException("No podes acceder a datos de otro viajero.");
        }
        return id;
    }

    /** a***@dominio.com: para los logs, sin exponer el email completo. */
    public static String enmascarar(String email) {
        if (email == null || !email.contains("@")) return "***";
        int arroba = email.indexOf('@');
        return (arroba > 0 ? email.substring(0, 1) : "") + "***" + email.substring(arroba);
    }
}
