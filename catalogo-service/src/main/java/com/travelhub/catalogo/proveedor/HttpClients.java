package com.travelhub.catalogo.proveedor;

import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.time.Duration;

final class HttpClients {

    private HttpClients() { }

    /** Timeouts cortos: si el proveedor no responde, se cae al plan B en vez de colgar la busqueda. */
    static SimpleClientHttpRequestFactory conTimeouts(Duration conexion, Duration lectura) {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(conexion);
        f.setReadTimeout(lectura);
        return f;
    }
}
