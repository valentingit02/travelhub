package com.travelhub.reservas.reserva;

import com.travelhub.common.events.*;
import com.travelhub.common.util.ValidadorFechas;
import com.travelhub.common.web.ConflictException;
import com.travelhub.common.web.NotFoundException;
import com.travelhub.reservas.cliente.CatalogoClient;
import com.travelhub.reservas.cliente.PreciosClient;
import com.travelhub.reservas.mensajeria.ReservasEventPublisher;
import com.travelhub.reservas.saga.ReservaSaga;
import com.travelhub.reservas.saga.SagaLogRepository;
import com.travelhub.reservas.viajero.Viajero;
import com.travelhub.reservas.viajero.ViajeroRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Patron Facade: un unico punto de entrada que esconde la orquestacion completa
 * (viajero -> verificacion de precios -> cotizacion -> Saga -> pago -> confirmacion).
 *
 * Reglas de consistencia:
 *  - El precio SIEMPRE sale del catalogo, nunca del request.
 *  - Cada cambio de estado y su evento se guardan en la MISMA transaccion (Outbox).
 *  - Las llamadas remotas (Saga) quedan FUERA de las transacciones de base de datos.
 */
@Service
public class ReservaFacade {

    private static final Logger log = LoggerFactory.getLogger(ReservaFacade.class);

    private final ReservaRepository reservas;
    private final ViajeroRepository viajeros;
    private final SagaLogRepository bitacora;
    private final CatalogoClient catalogo;
    private final PreciosClient precios;
    private final ReservaSaga saga;
    private final ReservasEventPublisher eventos;
    private final TransactionTemplate tx;

    public ReservaFacade(ReservaRepository reservas, ViajeroRepository viajeros, SagaLogRepository bitacora,
                         CatalogoClient catalogo, PreciosClient precios, ReservaSaga saga,
                         ReservasEventPublisher eventos, TransactionTemplate tx) {
        this.reservas = reservas;
        this.viajeros = viajeros;
        this.bitacora = bitacora;
        this.catalogo = catalogo;
        this.precios = precios;
        this.saga = saga;
        this.eventos = eventos;
        this.tx = tx;
    }

    public ReservaDtos.Response reservarPaquete(ReservaDtos.Request req, String idempotencyKey) {
        String clave = (idempotencyKey == null || idempotencyKey.isBlank()) ? null : idempotencyKey.trim();
        if (clave != null) {
            Optional<Reserva> existente = reservas.findByIdempotencyKey(clave);
            if (existente.isPresent()) {
                log.info("Idempotency-Key repetida: devuelvo la reserva {}", existente.get().getId());
                return respuesta(existente.get());
            }
        }
        ValidadorFechas.validarRango(req.desde(), req.hasta());
        Viajero viajero = viajeros.findById(req.viajeroId())
                .orElseThrow(() -> new NotFoundException("No existe el viajero " + req.viajeroId()));
        String destino = req.destino().toUpperCase();

        // 1. Verificar cada oferta en el catalogo: precio, disponibilidad y vigencia los decide el servidor
        List<CatalogoClient.Oferta> ofertas = new ArrayList<>();
        for (ReservaDtos.ItemRequest item : req.items()) {
            CatalogoClient.Oferta o = catalogo.verificar(item.productoId(), req.desde(), req.hasta());
            if (o.tipo() != item.tipo()) {
                throw new IllegalArgumentException("El producto " + item.productoId() + " no es de tipo " + item.tipo());
            }
            if (!destino.equalsIgnoreCase(o.destino())) {
                throw new IllegalArgumentException("\"" + o.nombre() + "\" no corresponde al destino " + destino);
            }
            ofertas.add(o);
        }

        // 2. Cotizar el paquete con los precios verificados (llamada sincronica a precios-service)
        List<PreciosClient.Item> aCotizar = ofertas.stream().map(o -> new PreciosClient.Item(o.tipo(), o.precio(),
                o.moneda(), req.desde(), req.hasta(), req.pasajeros(), o.ocupacion())).toList();
        PreciosClient.Paquete cotizacion = precios.cotizarPaquete(aCotizar);

        // 3. Crear la reserva PENDIENTE
        Reserva nueva = new Reserva(viajero.getId(), destino, req.desde(), req.hasta(), req.pasajeros());
        nueva.asignarClaveIdempotencia(clave);
        for (int i = 0; i < ofertas.size(); i++) {
            CatalogoClient.Oferta o = ofertas.get(i);
            ItemReserva item = new ItemReserva(o.tipo(), o.id(), o.nombre(), o.precio());
            item.setPrecioCotizado(cotizacion.items().get(i).precioFinal().multiply(cotizacion.factorPaquete())
                    .setScale(2, RoundingMode.HALF_UP));
            nueva.agregarItem(item);
        }
        nueva.setTotal(cotizacion.total(), cotizacion.moneda());
        Reserva reserva;
        try {
            reserva = reservas.saveAndFlush(nueva);
        } catch (DataIntegrityViolationException e) {
            if (clave == null) throw e;
            // Dos requests con la misma clave llegaron a la vez: gana el primero
            return respuesta(reservas.findByIdempotencyKey(clave).orElseThrow(() -> e));
        }
        saga.registrar(reserva, "COTIZAR", "OK", "Precios verificados en catalogo. Total " + cotizacion.total()
                + " " + cotizacion.moneda() + " (" + cotizacion.motivoDescuento() + ")");

        // 4. Saga: reservar cada item en su proveedor (con compensacion si algo falla)
        String fallo = saga.reservarItems(reserva, new CatalogoClient.Titular(viajero.getNombre(),
                viajero.getApellido(), viajero.getEmail(), viajero.getFechaNacimiento()));

        final Reserva r = reserva;
        if (fallo != null) {
            tx.executeWithoutResult(s -> {
                r.cambiarEstado(EstadoReserva.FALLIDA, fallo);
                reservas.save(r);
                eventos.publicar(Eventos.RESERVA_CANCELADA, new ReservaCanceladaEvent(r.getId(),
                        viajero.getEmail(), viajero.getNombre(), EstadoReserva.FALLIDA.name(), fallo));
            });
            return obtener(r.getId());
        }

        // 5. Pedir el cobro de forma asincronica: estado + evento en la misma transaccion (Outbox)
        tx.executeWithoutResult(s -> {
            reservas.save(r);
            eventos.publicar(Eventos.RESERVA_CREADA, new ReservaCreadaEvent(r.getId(), viajero.getId(),
                    viajero.getEmail(), r.getTotal(), r.getMoneda()));
            saga.registrar(r, "SOLICITAR_PAGO", "OK", "Evento " + Eventos.RESERVA_CREADA + " guardado en outbox");
        });
        return obtener(r.getId());
    }

    @Transactional
    public void pagoAprobado(PagoAprobadoEvent e) {
        Reserva r = reservas.findById(e.reservaId()).orElse(null);
        if (r == null || r.getEstado() != EstadoReserva.PENDIENTE) {
            // idempotencia: si el mensaje llega dos veces, o la reserva ya vencio, no se confirma de nuevo
            log.warn("PagoAprobado ignorado para reserva {} (estado {}). Si vencio, requiere reembolso manual.",
                    e.reservaId(), r == null ? "inexistente" : r.getEstado());
            return;
        }
        r.cambiarEstado(EstadoReserva.PAGADA, null);
        saga.registrar(r, "PAGO", "OK", "Pago " + e.referencia());
        r.cambiarEstado(EstadoReserva.CONFIRMADA, null);
        reservas.save(r);
        saga.registrar(r, "CONFIRMAR", "OK", "Reserva confirmada");

        Viajero v = viajeros.findById(r.getViajeroId()).orElseThrow();
        eventos.publicar(Eventos.RESERVA_CONFIRMADA, new ReservaConfirmadaEvent(r.getId(), v.getEmail(),
                v.getNombre(), r.getDestino(), r.getDesde(), r.getHasta(), r.getPasajeros(),
                r.getItems().stream().map(i -> new ReservaConfirmadaEvent.Item(i.getTipo().name(),
                        i.getDescripcion(), i.getPrecioCotizado())).toList(),
                r.getTotal(), r.getMoneda()));
    }

    public void pagoRechazado(PagoRechazadoEvent e) {
        Reserva r = reservas.findById(e.reservaId()).orElse(null);
        if (r == null || r.getEstado() != EstadoReserva.PENDIENTE) return;
        saga.registrar(r, "PAGO", "FALLO", e.motivo());
        saga.compensar(r);
        cerrar(r, EstadoReserva.FALLIDA, "Pago rechazado: " + e.motivo());
    }

    /** La invoca ReservasVencidasJob cuando el pago no llego a tiempo. */
    public void expirar(Long id, Duration vencimiento) {
        Reserva r = reservas.findById(id).orElse(null);
        if (r == null || r.getEstado() != EstadoReserva.PENDIENTE) return;
        String motivo = "No se recibio el pago en " + vencimiento.toMinutes() + " minutos";
        log.warn("Reserva {} vencida: {}", id, motivo);
        saga.registrar(r, "VENCIMIENTO", "FALLO", motivo);
        saga.compensar(r);
        cerrar(r, EstadoReserva.FALLIDA, motivo);
    }

    public ReservaDtos.Response cancelar(Long id) {
        Reserva r = buscar(id);
        if (r.getEstado() == EstadoReserva.CANCELADA || r.getEstado() == EstadoReserva.FALLIDA) {
            throw new ConflictException("La reserva " + id + " ya esta " + r.getEstado());
        }
        saga.compensar(r);
        saga.registrar(r, "CANCELAR", "OK", "Cancelada por el viajero");
        cerrar(r, EstadoReserva.CANCELADA, "Cancelada por el viajero");
        return obtener(id);
    }

    /** Estado final + evento de cancelacion, atomicos (Outbox). */
    private void cerrar(Reserva r, EstadoReserva estado, String motivo) {
        Viajero v = viajeros.findById(r.getViajeroId()).orElseThrow();
        tx.executeWithoutResult(s -> {
            r.cambiarEstado(estado, motivo);
            reservas.save(r);
            eventos.publicar(Eventos.RESERVA_CANCELADA, new ReservaCanceladaEvent(r.getId(), v.getEmail(),
                    v.getNombre(), estado.name(), motivo));
        });
    }

    public ReservaDtos.Response obtener(Long id) {
        return respuesta(buscar(id));
    }

    public List<ReservaDtos.Response> listar(Long viajeroId) {
        List<Reserva> lista = viajeroId == null ? reservas.findAllByOrderByCreadaEnDesc()
                : reservas.findByViajeroIdOrderByCreadaEnDesc(viajeroId);
        return lista.stream().map(this::respuesta).toList();
    }

    private Reserva buscar(Long id) {
        return reservas.findById(id).orElseThrow(() -> new NotFoundException("No existe la reserva " + id));
    }

    private ReservaDtos.Response respuesta(Reserva r) {
        return ReservaDtos.Response.from(r, bitacora.findByReservaIdOrderByIdAsc(r.getId()));
    }
}
