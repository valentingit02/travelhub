package com.travelhub.reservas.reserva;

import com.travelhub.common.domain.TipoProducto;
import com.travelhub.reservas.compartida.EstadoParticipante;
import com.travelhub.reservas.compartida.Participante;
import com.travelhub.reservas.saga.SagaLog;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class ReservaDtos {

    private ReservaDtos() { }

    /** El cliente SOLO elige que producto quiere: precio y descripcion los pone el servidor. */
    public record ItemRequest(@NotNull TipoProducto tipo, @NotBlank @Size(max = 1000) String productoId) { }

    /** compartirCon: emails de los amigos que pagan su parte (vacio = viaje individual). */
    public record Request(@NotNull Long viajeroId, @NotBlank @Size(min = 3, max = 3) String destino,
                          @NotNull @FutureOrPresent LocalDate desde, @NotNull LocalDate hasta,
                          @NotNull @Min(1) @Max(9) Integer pasajeros,
                          @NotEmpty @Size(max = 8) List<@Valid ItemRequest> items,
                          @Size(max = 8) List<@NotBlank @Email String> compartirCon) { }

    public record ItemResponse(Long id, TipoProducto tipo, String productoId, String descripcion,
                               BigDecimal precioBase, BigDecimal precioCotizado, String refExterna, EstadoItem estado) {
        static ItemResponse from(ItemReserva i) {
            return new ItemResponse(i.getId(), i.getTipo(), i.getProductoId(), i.getDescripcion(), i.getPrecioBase(),
                    i.getPrecioCotizado(), i.getRefExterna(), i.getEstado());
        }
    }

    public record PasoSaga(String paso, String estado, String detalle, Instant fecha) {
        static PasoSaga from(SagaLog l) {
            return new PasoSaga(l.getPaso(), l.getEstado(), l.getDetalle(), l.getFecha());
        }
    }

    public record ParticipanteResponse(Long id, String nombre, String email, BigDecimal monto,
                                       EstadoParticipante estado, boolean organizador, String token, String motivo) {
        static ParticipanteResponse from(Participante p) {
            return new ParticipanteResponse(p.getId(), p.getNombre(), p.getEmail(), p.getMonto(), p.getEstado(),
                    p.isOrganizador(), p.getToken(), p.getMotivo());
        }
    }

    public record Response(Long id, Long viajeroId, String destino, LocalDate desde, LocalDate hasta,
                           Integer pasajeros, EstadoReserva estado, BigDecimal total, String moneda, String motivo,
                           Instant creadaEn, boolean compartida, Instant venceEn, BigDecimal creditoAplicado,
                           List<ItemResponse> items, List<ParticipanteResponse> participantes, List<PasoSaga> saga) {
        public static Response from(Reserva r, List<SagaLog> log, List<Participante> participantes) {
            return new Response(r.getId(), r.getViajeroId(), r.getDestino(), r.getDesde(), r.getHasta(),
                    r.getPasajeros(), r.getEstado(), r.getTotal(), r.getMoneda(), r.getMotivo(), r.getCreadaEn(),
                    r.isCompartida(), r.getVenceEn(), r.getCreditoAplicado(),
                    r.getItems().stream().map(ItemResponse::from).toList(),
                    participantes.stream().map(ParticipanteResponse::from).toList(),
                    log.stream().map(PasoSaga::from).toList());
        }
    }
}
