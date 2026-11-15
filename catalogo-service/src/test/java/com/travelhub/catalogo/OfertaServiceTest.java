package com.travelhub.catalogo;

import com.travelhub.catalogo.auto.AutoService;
import com.travelhub.catalogo.historial.HistorialPrecioRepository;
import com.travelhub.catalogo.mensajeria.EventPublisher;
import com.travelhub.catalogo.moneda.TipoCambioService;
import com.travelhub.catalogo.oferta.OfertaPublicada;
import com.travelhub.catalogo.oferta.OfertaPublicadaRepository;
import com.travelhub.catalogo.oferta.OfertaService;
import com.travelhub.catalogo.proveedor.OfertaProveedor;
import com.travelhub.common.domain.TipoProducto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Registro de ofertas: una sola lectura y solo se escribe lo que cambio o esta por vencer (rendimiento bajo carga). */
class OfertaServiceTest {

    private OfertaPublicadaRepository repo;
    private EventPublisher publisher;
    private OfertaService service;

    @BeforeEach
    void setUp() {
        repo = mock(OfertaPublicadaRepository.class);
        publisher = mock(EventPublisher.class);
        service = new OfertaService(repo, mock(HistorialPrecioRepository.class), publisher,
                mock(AutoService.class), mock(TipoCambioService.class), Duration.ofMinutes(60));
    }

    private static OfertaProveedor oferta(String id, String precio) {
        return new OfertaProveedor(id, TipoProducto.HOTEL, "MOCK", "Hotel " + id, "BRC", new BigDecimal(precio),
                "USD", "noches", 0.6, Map.of());
    }

    private static OfertaPublicada guardada(OfertaProveedor o, Duration leQueda) {
        return new OfertaPublicada(o, Instant.now().plus(leQueda));
    }

    @Test
    void sinCambiosYConVigenciaNoEscribeNada() {
        OfertaProveedor o = oferta("H1", "100");
        when(repo.findAllById(any())).thenReturn(List.of(guardada(o, Duration.ofMinutes(55))));

        assertEquals(1, service.publicar(List.of(o)).size());

        verify(repo, never()).save(any());
        verifyNoInteractions(publisher);
    }

    @Test
    void consultaTodasLasOfertasDeUnaSolaVez() {
        when(repo.findAllById(any())).thenReturn(List.of());
        service.publicar(List.of(oferta("H1", "100"), oferta("H2", "120"), oferta("H3", "90")));
        verify(repo, times(1)).findAllById(argThat(ids -> ((List<?>) ids).size() == 3));
        verify(repo, never()).findById(any());
    }

    @Test
    void ofertaNuevaSeGuardaYAvisaALaIa() {
        when(repo.findAllById(any())).thenReturn(List.of());
        service.publicar(List.of(oferta("H1", "100")));
        verify(repo).save(any(OfertaPublicada.class));
        verify(publisher).precioCambiado(argThat(e -> e.precioAnterior() == null));
    }

    @Test
    void siCambiaElPrecioSeActualizaYAvisaALaIa() {
        OfertaProveedor antes = oferta("H1", "100");
        when(repo.findAllById(any())).thenReturn(List.of(guardada(antes, Duration.ofMinutes(55))));

        service.publicar(List.of(oferta("H1", "80")));

        verify(repo).save(argThat(p -> p.getPrecio().compareTo(new BigDecimal("80")) == 0));
        verify(publisher).precioCambiado(argThat(e -> e.precioAnterior().compareTo(new BigDecimal("100")) == 0));
    }

    @Test
    void siCambiaOtroDatoConElMismoPrecioTambienSeActualiza() {
        // Caso real del mock: el mismo id aparece en otro destino. Si no se reescribe, verificar() lo rechaza.
        OfertaProveedor enMendoza = new OfertaProveedor("H1", TipoProducto.HOTEL, "MOCK", "Hotel H1", "MDZ",
                new BigDecimal("100"), "USD", "noches", 0.6, Map.of());
        OfertaPublicada p = guardada(enMendoza, Duration.ofMinutes(55));
        when(repo.findAllById(any())).thenReturn(List.of(p));

        service.publicar(List.of(oferta("H1", "100")));   // mismo precio, pero destino BRC

        verify(repo).save(p);
        assertEquals("BRC", p.getDestino());
        verifyNoInteractions(publisher);                   // el precio no cambio: la IA no se entera
    }

    @Test
    void siEstaPorVencerSeRenuevaSinAvisar() {
        OfertaProveedor o = oferta("H1", "100");
        OfertaPublicada p = guardada(o, Duration.ofMinutes(10));   // menos de media vigencia (30 min)
        when(repo.findAllById(any())).thenReturn(List.of(p));

        service.publicar(List.of(o));

        verify(repo).save(p);
        assertTrue(p.getExpiraEn().isAfter(Instant.now().plus(Duration.ofMinutes(55))));
        verifyNoInteractions(publisher);
    }

    @Test
    void laBloqueadaPorLaIaNoSeMuestra() {
        OfertaProveedor o = oferta("H1", "1");
        OfertaPublicada p = guardada(o, Duration.ofMinutes(55));
        p.bloquear("precio anomalo");
        when(repo.findAllById(any())).thenReturn(List.of(p));

        assertTrue(service.publicar(List.of(o)).isEmpty());
        verify(repo, never()).save(any());
    }
}
