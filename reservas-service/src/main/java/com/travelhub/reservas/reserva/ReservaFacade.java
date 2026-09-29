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
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Patron Facade: un unico punto de entrada que esconde la orquestacion completa
 * (viajero -> cotizacion -> Saga en proveedores -> evento de pago -> confirmacion).
 */
@Service
public class ReservaFacade {

    private static final Logger log = LoggerFactory.getLogger(ReservaFacade.class);

    private final ReservaRepository reservas;
    private final ViajeroRepository viajeros;
    private final SagaLogRepository bitacora;
    private final PreciosClient precios;
    private final ReservaSaga saga;
    private final ReservasEventPublisher eventos;

    public ReservaFacade(ReservaRepository reservas, ViajeroRepository viajeros, SagaLogRepository bitacora,
                         PreciosClient precios, ReservaSaga saga, ReservasEventPublisher eventos) {
        this.reservas = reservas;
        this.viajeros = viajeros;
        this.bitacora = bitacora;
        this.precios = precios;
        this.saga = saga;
        this.eventos = eventos;
    }

    public ReservaDtos.Response reservarPaquete(ReservaDtos.Request req) {
        ValidadorFechas.validarRango(req.desde(), req.hasta());
        Viajero viajero = viajeros.findById(req.viajeroId())
                .orElseThrow(() -> new NotFoundException("No existe el viajero " + req.viajeroId()));

        // 1. Cotizar el paquete (llamada sincronica a precios-service)
        List<PreciosClient.Item> aCotizar = req.items().stream().map(i -> new PreciosClient.Item(i.tipo(),
                i.precioBase(), i.moneda(), req.desde(), req.hasta(), req.pasajeros(), i.ocupacion())).toList();
        PreciosClient.Paquete cotizacion = precios.cotizarPaquete(aCotizar);

        // 2. Crear la reserva PENDIENTE
        Reserva reserva = new Reserva(viajero.getId(), req.destino().toUpperCase(), req.desde(), req.hasta(),
                req.pasajeros());
        for (int i = 0; i < req.items().size(); i++) {
            ReservaDtos.ItemRequest ir = req.items().get(i);
            ItemReserva item = new ItemReserva(ir.tipo(), ir.productoId(), ir.descripcion(), ir.precioBase());
            item.setPrecioCotizado(cotizacion.items().get(i).precioFinal().multiply(cotizacion.factorPaquete())
                    .setScale(2, java.math.RoundingMode.HALF_UP));
            reserva.agregarItem(item);
        }
        reserva.setTotal(cotizacion.total(), cotizacion.moneda());
        reserva = reservas.save(reserva);
        saga.registrar(reserva, "COTIZAR", "OK", "Total " + cotizacion.total() + " " + cotizacion.moneda()
                + " (" + cotizacion.motivoDescuento() + ")");

        // 3. Saga: reservar cada item en su proveedor (con compensacion si algo falla)
        String fallo = saga.reservarItems(reserva, new CatalogoClient.Titular(viajero.getNombre(),
                viajero.getApellido(), viajero.getEmail(), viajero.getFechaNacimiento()));
        if (fallo != null) {
            reserva.cambiarEstado(EstadoReserva.FALLIDA, fallo);
            reserva = reservas.save(reserva);
            eventos.publicar(Eventos.RESERVA_CANCELADA, new ReservaCanceladaEvent(reserva.getId(),
                    viajero.getEmail(), viajero.getNombre(), EstadoReserva.FALLIDA.name(), fallo));
            return respuesta(reserva);
        }
        reserva = reservas.save(reserva);

        // 4. Pedir el cobro de forma asincronica (pagos-service consume este evento)
        eventos.publicar(Eventos.RESERVA_CREADA, new ReservaCreadaEvent(reserva.getId(), viajero.getId(),
                viajero.getEmail(), reserva.getTotal(), reserva.getMoneda()));
        saga.registrar(reserva, "SOLICITAR_PAGO", "OK", "Evento " + Eventos.RESERVA_CREADA + " publicado");
        return respuesta(reserva);
    }

    public void pagoAprobado(PagoAprobadoEvent e) {
        Reserva r = reservas.findById(e.reservaId()).orElse(null);
        if (r == null || r.getEstado() != EstadoReserva.PENDIENTE) {
            log.warn("PagoAprobado ignorado para reserva {} (no esta PENDIENTE)", e.reservaId());
            return; // idempotencia: si el mensaje llega dos veces no pasa nada
        }
        r.cambiarEstado(EstadoReserva.PAGADA, null);
        saga.registrar(r, "PAGO", "OK", "Pago " + e.referencia());
        r.cambiarEstado(EstadoReserva.CONFIRMADA, null);
        r = reservas.save(r);
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
        r.cambiarEstado(EstadoReserva.FALLIDA, "Pago rechazado: " + e.motivo());
        r = reservas.save(r);
        Viajero v = viajeros.findById(r.getViajeroId()).orElseThrow();
        eventos.publicar(Eventos.RESERVA_CANCELADA, new ReservaCanceladaEvent(r.getId(), v.getEmail(),
                v.getNombre(), r.getEstado().name(), r.getMotivo()));
    }

    public ReservaDtos.Response cancelar(Long id) {
        Reserva r = buscar(id);
        if (r.getEstado() == EstadoReserva.CANCELADA || r.getEstado() == EstadoReserva.FALLIDA) {
            throw new ConflictException("La reserva " + id + " ya esta " + r.getEstado());
        }
        saga.compensar(r);
        r.cambiarEstado(EstadoReserva.CANCELADA, "Cancelada por el viajero");
        r = reservas.save(r);
        saga.registrar(r, "CANCELAR", "OK", "Cancelada por el viajero");
        Viajero v = viajeros.findById(r.getViajeroId()).orElseThrow();
        eventos.publicar(Eventos.RESERVA_CANCELADA, new ReservaCanceladaEvent(r.getId(), v.getEmail(),
                v.getNombre(), r.getEstado().name(), r.getMotivo()));
        return respuesta(r);
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
