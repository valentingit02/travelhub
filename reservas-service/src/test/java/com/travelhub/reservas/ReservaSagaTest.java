package com.travelhub.reservas;

import com.travelhub.common.domain.TipoProducto;
import com.travelhub.common.web.ExternalServiceException;
import com.travelhub.reservas.cliente.CatalogoClient;
import com.travelhub.reservas.reserva.EstadoItem;
import com.travelhub.reservas.reserva.ItemReserva;
import com.travelhub.reservas.reserva.Reserva;
import com.travelhub.reservas.saga.ReservaSaga;
import com.travelhub.reservas.saga.SagaLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Prueba unitaria de la Saga: si falla el hotel, se cancela el vuelo que ya estaba reservado. */
class ReservaSagaTest {

    private CatalogoClient catalogo;
    private ReservaSaga saga;
    private Reserva reserva;
    private final CatalogoClient.Titular titular =
            new CatalogoClient.Titular("Ana", "Perez", "ana@test.com", LocalDate.of(1995, 5, 10));

    @BeforeEach
    void setUp() {
        catalogo = mock(CatalogoClient.class);
        saga = new ReservaSaga(catalogo, mock(SagaLogRepository.class));
        reserva = new Reserva(1L, "BRC", LocalDate.now().plusDays(30), LocalDate.now().plusDays(35), 2);
        reserva.agregarItem(new ItemReserva(TipoProducto.VUELO, "MOCK-VUELO-1", "Vuelo", new BigDecimal("150")));
        reserva.agregarItem(new ItemReserva(TipoProducto.HOTEL, "MOCK-HOTEL-FALLA", "Hotel", new BigDecimal("80")));
    }

    @Test
    void todoOkNoCompensa() {
        when(catalogo.reservar(any(), anyString(), any(), any(), anyInt(), anyString(), any())).thenReturn("REF-1");

        assertNull(saga.reservarItems(reserva, titular));
        assertTrue(reserva.getItems().stream().allMatch(i -> i.getEstado() == EstadoItem.RESERVADO));
        verify(catalogo, never()).cancelar(any(), anyString(), anyString());
    }

    @Test
    void siFallaElHotelCancelaElVuelo() {
        when(catalogo.reservar(eq(TipoProducto.VUELO), anyString(), any(), any(), anyInt(), anyString(), any()))
                .thenReturn("VUELO-REF");
        when(catalogo.reservar(eq(TipoProducto.HOTEL), anyString(), any(), any(), anyInt(), anyString(), any()))
                .thenThrow(new ExternalServiceException("sin disponibilidad"));

        String fallo = saga.reservarItems(reserva, titular);

        assertNotNull(fallo);
        verify(catalogo).cancelar(eq(TipoProducto.VUELO), eq("VUELO-REF"), anyString());
        assertEquals(EstadoItem.CANCELADO, reserva.getItems().get(0).getEstado());
        assertEquals(EstadoItem.FALLIDO, reserva.getItems().get(1).getEstado());
    }
}
