package com.travelhub.precios;

import com.travelhub.common.domain.TipoProducto;
import com.travelhub.precios.cotizacion.CotizacionRequest;
import com.travelhub.precios.cotizacion.CotizacionResponse;
import com.travelhub.precios.cotizacion.CotizadorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Evidencia del TP Inicial: el mismo componente invocado
 *  (1) localmente (llamada directa en la JVM) y
 *  (2) remotamente (HTTP REST y SOAP).
 * Imprime los tiempos promedio para el informe.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class InvocacionLocalVsRemotaTest {

    private static final int N = 50;

    @Autowired CotizadorService cotizador;
    @Autowired TestRestTemplate http;

    private CotizacionRequest request() {
        LocalDate inicio = LocalDate.now().plusDays(40);
        return new CotizacionRequest(TipoProducto.HOTEL, new BigDecimal("100"), "USD",
                inicio, inicio.plusDays(3), 2, 0.6);
    }

    @Test
    void mismoResultadoLocalYRemotoConTiempos() {
        CotizacionRequest req = request();

        CotizacionResponse local = cotizador.cotizar(req);
        CotizacionResponse remoto = http.postForObject("/api/precios/cotizar", req, CotizacionResponse.class);
        assertEquals(local.precioFinal(), remoto.precioFinal());

        long t0 = System.nanoTime();
        for (int i = 0; i < N; i++) cotizador.cotizar(req);
        double msLocal = (System.nanoTime() - t0) / 1e6 / N;

        t0 = System.nanoTime();
        for (int i = 0; i < N; i++) http.postForObject("/api/precios/cotizar", req, CotizacionResponse.class);
        double msRest = (System.nanoTime() - t0) / 1e6 / N;

        String soap = """
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:c="http://travelhub.com/precios/soap">
                  <soapenv:Body><c:CotizarProductoRequest><c:item>
                    <c:tipoProducto>HOTEL</c:tipoProducto><c:precioBase>100</c:precioBase><c:moneda>USD</c:moneda>
                    <c:fechaInicio>%s</c:fechaInicio><c:fechaFin>%s</c:fechaFin>
                    <c:pasajeros>2</c:pasajeros><c:ocupacion>0.6</c:ocupacion>
                  </c:item></c:CotizarProductoRequest></soapenv:Body>
                </soapenv:Envelope>""".formatted(req.fechaInicio(), req.fechaFin());
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.TEXT_XML);

        ResponseEntity<String> soapResp = http.postForEntity("/ws", new HttpEntity<>(soap, h), String.class);
        assertTrue(soapResp.getBody().contains(local.precioFinal().toPlainString()),
                "La respuesta SOAP debe traer el mismo precio final");

        t0 = System.nanoTime();
        for (int i = 0; i < N; i++) http.postForEntity("/ws", new HttpEntity<>(soap, h), String.class);
        double msSoap = (System.nanoTime() - t0) / 1e6 / N;

        System.out.printf("%n=== Invocacion local vs remota (%d llamadas) ===%n", N);
        System.out.printf("Local (misma JVM): %.3f ms%n", msLocal);
        System.out.printf("Remota REST/JSON:  %.3f ms%n", msRest);
        System.out.printf("Remota SOAP/XML:   %.3f ms%n%n", msSoap);
        assertTrue(msLocal < msRest);
    }

    @Test
    void wsdlPublicado() {
        String wsdl = http.getForObject("/ws/cotizacion.wsdl", String.class);
        assertNotNull(wsdl);
        assertTrue(wsdl.contains("CotizarProducto"));
    }
}
