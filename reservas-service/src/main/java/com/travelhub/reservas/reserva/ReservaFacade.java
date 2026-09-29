package com.travelhub.reservas.reserva;

import com.travelhub.common.events.*;
import com.travelhub.common.util.ValidadorFechas;
import com.travelhub.common.web.ConflictException;
import com.travelhub.common.web.NotFoundException;
import com.travelhub.reservas.cliente.CatalogoClient;
import com.travelhub.reservas.cliente.PreciosClient;
import com.travelhub.reservas.compartida.*;
import com.travelhub.reservas.credito.CreditoService;
import com.travelhub.reservas.mensajeria.ReservasEventPublisher;
import com.travelhub.reservas.saga.ReservaSaga;
import com.travelhub.reservas.saga.SagaLogRepository;
import com.travelhub.reservas.viajero.Viajero;
import com.travelhub.reservas.viajero.ViajeroRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Patron Facade: un unico punto de entrada que esconde la orquestacion completa
 * (viajero -> verificacion de precios -> cotizacion -> Saga -> credito -> pagos por participante -> confirmacion).
 *
 * Reglas de consistencia:
 *  - El precio SIEMPRE sale del catalogo, nunca del request.
 *  - Cada cambio de estado y su evento se guardan en la MISMA transaccion (Outbox).
 *  - Las llamadas remotas (Saga) quedan FUERA de las transacciones de base de datos.
 *  - Viaje compartido: la reserva se confirma cuando pagaron todos; si vence antes, se compensa y se reembolsa.
 */
@Service
public class ReservaFacade {

    private static final Logger log = LoggerFactory.getLogger(ReservaFacade.class);
    private static final String PAGO_CON_CREDITO = "CREDITO";

    private final ReservaRepository reservas;
    private final ViajeroRepository viajeros;
    private final SagaLogRepository bitacora;
    private final ParticipanteRepository participantes;
    private final CatalogoClient catalogo;
    private final PreciosClient precios;
    private final ReservaSaga saga;
    private final ReservasEventPublisher eventos;
    private final CreditoService creditos;
    private final TransactionTemplate tx;
    private final Duration plazoIndividual;
    private final Duration plazoCompartido;

    public ReservaFacade(ReservaRepository reservas, ViajeroRepository viajeros, SagaLogRepository bitacora,
                         ParticipanteRepository participantes, CatalogoClient catalogo, PreciosClient precios,
                         ReservaSaga saga, ReservasEventPublisher eventos, CreditoService creditos,
                         TransactionTemplate tx,
                         @Value("${travelhub.reservas.vencimiento:15m}") Duration plazoIndividual,
                         @Value("${travelhub.reservas.vencimiento-compartida:24h}") Duration plazoCompartido) {
        this.reservas = reservas;
        this.viajeros = viajeros;
        this.bitacora = bitacora;
        this.participantes = participantes;
        this.catalogo = catalogo;
        this.precios = precios;
        this.saga = saga;
        this.eventos = eventos;
        this.creditos = creditos;
        this.tx = tx;
        this.plazoIndividual = plazoIndividual;
        this.plazoCompartido = plazoCompartido;
    }

    // ------------------------------------------------------------------ reservar

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
        List<String> invitados = normalizarInvitados(req.compartirCon(), viajero.getEmail());
        boolean compartida = !invitados.isEmpty();

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

        // 2. Cotizar el paquete con los precios verificados
        List<PreciosClient.Item> aCotizar = ofertas.stream().map(o -> new PreciosClient.Item(o.tipo(), o.precio(),
                o.moneda(), req.desde(), req.hasta(), req.pasajeros(), o.ocupacion())).toList();
        PreciosClient.Paquete cotizacion = precios.cotizarPaquete(aCotizar);

        // 3. Crear la reserva PENDIENTE
        Reserva nueva = new Reserva(viajero.getId(), destino, req.desde(), req.hasta(), req.pasajeros());
        nueva.asignarClaveIdempotencia(clave);
        nueva.configurarPago(compartida, compartida ? plazoCompartido : plazoIndividual);
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
                        viajero.getEmail(), viajero.getNombre(), EstadoReserva.FALLIDA.name(), fallo, List.of()));
            });
            return obtener(r.getId());
        }

        // 5. Credito + partes a pagar + pedidos de cobro e invitaciones (todo atomico via Outbox)
        tx.executeWithoutResult(s -> iniciarCobro(r, viajero, invitados));
        return obtener(r.getId());
    }

    private List<String> normalizarInvitados(List<String> emails, String organizador) {
        if (emails == null) return List.of();
        LinkedHashSet<String> unicos = new LinkedHashSet<>();
        for (String e : emails) {
            if (e == null || e.isBlank()) continue;
            String m = e.trim().toLowerCase();
            if (m.equalsIgnoreCase(organizador)) {
                throw new IllegalArgumentException("No hace falta invitarte a vos mismo: ya sos el organizador");
            }
            unicos.add(m);
        }
        return List.copyOf(unicos);
    }

    private void iniciarCobro(Reserva r, Viajero organizador, List<String> invitados) {
        BigDecimal credito = BigDecimal.ZERO;
        if (!r.isCompartida()) {
            credito = creditos.aplicar(organizador.getId(), r.getId(), r.getTotal(), r.getMoneda());
            if (credito.signum() > 0) {
                r.aplicarCredito(credito);
                saga.registrar(r, "CREDITO", "OK", "Se aplicaron " + credito + " " + r.getMoneda() + " de credito a favor");
            }
        }
        BigDecimal aPagar = r.getTotal().subtract(credito);
        List<BigDecimal> partes = Reparto.partes(aPagar, invitados.size() + 1);

        Participante org = participantes.save(new Participante(r.getId(), organizador.getNombre(),
                organizador.getEmail(), partes.get(0), true));
        List<Participante> otros = new ArrayList<>();
        for (int i = 0; i < invitados.size(); i++) {
            otros.add(participantes.save(new Participante(r.getId(), null, invitados.get(i), partes.get(i + 1), false)));
        }
        reservas.save(r);

        if (aPagar.signum() == 0) {
            org.marcarPagado(PAGO_CON_CREDITO);
            participantes.save(org);
            saga.registrar(r, "PAGO", "OK", "Pagada completamente con credito");
            confirmar(r);
            return;
        }
        solicitarPago(r, org);
        for (Participante p : otros) {
            eventos.publicar(Eventos.INVITACION_VIAJE, new InvitacionViajeEvent(r.getId(), p.getToken(), p.getEmail(),
                    organizador.getNombre(), r.getDestino(), r.getDesde(), r.getHasta(), p.getMonto(), r.getMoneda(),
                    r.getVenceEn()));
        }
        if (!otros.isEmpty()) {
            saga.registrar(r, "INVITAR", "OK", otros.size() + " invitacion(es) enviada(s). Plazo para pagar: "
                    + plazoCompartido.toHours() + " h");
        }
    }

    private void solicitarPago(Reserva r, Participante p) {
        p.marcarProcesando();
        participantes.save(p);
        eventos.publicar(Eventos.PAGO_SOLICITADO, new PagoSolicitadoEvent(r.getId(), p.getId(), p.getEmail(),
                p.getMonto(), r.getMoneda()));
        saga.registrar(r, "SOLICITAR_PAGO", "OK", "Cobro de " + p.getMonto() + " " + r.getMoneda() + " a " + p.etiqueta());
    }

    // ------------------------------------------------------------------ viaje compartido

    public CompartidaDtos.Vista vista(String token) {
        Participante p = participantes.findByToken(token)
                .orElseThrow(() -> new NotFoundException("El link de pago no es valido"));
        Reserva r = buscar(p.getReservaId());
        List<Participante> todos = participantes.findByReservaIdOrderByIdAsc(r.getId());
        String organizador = todos.stream().filter(Participante::isOrganizador).map(Participante::etiqueta)
                .findFirst().orElse("Tu amigo");
        long pagaron = todos.stream().filter(x -> x.getEstado() == EstadoParticipante.PAGADO).count();
        return new CompartidaDtos.Vista(p.getToken(), r.getId(), r.getDestino(), r.getDesde(), r.getHasta(),
                r.getPasajeros(), organizador, p.getEmail(), p.getNombre(), p.getMonto(), r.getMoneda(), p.getEstado(),
                p.getMotivo(), r.getEstado(), r.getVenceEn(), pagaron, todos.size(), r.getTotal(),
                r.getItems().stream().map(i -> new CompartidaDtos.Item(i.getTipo().name(), i.getDescripcion())).toList());
    }

    public CompartidaDtos.Vista pagarParte(String token, String nombre) {
        Participante p = participantes.findByToken(token)
                .orElseThrow(() -> new NotFoundException("El link de pago no es valido"));
        Reserva r = buscar(p.getReservaId());
        if (r.getEstado() != EstadoReserva.PENDIENTE) {
            throw new ConflictException("Esta reserva ya esta " + r.getEstado().name().toLowerCase());
        }
        if (r.getVenceEn().isBefore(Instant.now())) {
            throw new ConflictException("El plazo para pagar ya vencio");
        }
        if (p.getEstado() == EstadoParticipante.PAGADO || p.getEstado() == EstadoParticipante.PROCESANDO) {
            return vista(token);
        }
        tx.executeWithoutResult(s -> {
            if (nombre != null && !nombre.isBlank()) p.setNombre(nombre.trim());
            solicitarPago(r, p);
        });
        return vista(token);
    }

    // ------------------------------------------------------------------ resultados de pago

    public void pagoAprobado(PagoAprobadoEvent e) {
        Reserva r = reservas.findById(e.reservaId()).orElse(null);
        Participante p = e.participanteId() == null ? null : participantes.findById(e.participanteId()).orElse(null);
        if (r == null || p == null) {
            log.warn("PagoAprobado sin reserva o participante ({} / {}): ignorado", e.reservaId(), e.participanteId());
            return;
        }
        if (p.getEstado() == EstadoParticipante.PAGADO) return; // mensaje duplicado

        if (r.getEstado() != EstadoReserva.PENDIENTE) {
            // Llego tarde: la reserva ya vencio o se cancelo -> se devuelve lo cobrado
            tx.executeWithoutResult(s -> {
                p.marcarPagado(e.referencia());
                reembolsar(r, p, "El pago llego cuando la reserva ya estaba " + r.getEstado().name().toLowerCase());
            });
            return;
        }
        tx.executeWithoutResult(s -> {
            p.marcarPagado(e.referencia());
            participantes.save(p);
            saga.registrar(r, "PAGO", "OK", "Pago de " + p.etiqueta() + " (" + p.getMonto() + " " + r.getMoneda()
                    + ") ref " + e.referencia());
            boolean todos = participantes.findByReservaIdOrderByIdAsc(r.getId()).stream()
                    .allMatch(x -> x.getEstado() == EstadoParticipante.PAGADO);
            if (todos) confirmar(r);
        });
    }

    private void confirmar(Reserva r) {
        r.cambiarEstado(EstadoReserva.PAGADA, null);
        r.cambiarEstado(EstadoReserva.CONFIRMADA, null);
        reservas.save(r);
        saga.registrar(r, "CONFIRMAR", "OK", r.isCompartida() ? "Pagaron todos: reserva confirmada" : "Reserva confirmada");

        Viajero v = viajeros.findById(r.getViajeroId()).orElseThrow();
        List<String> acompanantes = participantes.findByReservaIdOrderByIdAsc(r.getId()).stream()
                .filter(x -> !x.isOrganizador()).map(Participante::getEmail).toList();
        eventos.publicar(Eventos.RESERVA_CONFIRMADA, new ReservaConfirmadaEvent(r.getId(), v.getEmail(),
                v.getNombre(), r.getDestino(), r.getDesde(), r.getHasta(), r.getPasajeros(),
                r.getItems().stream().map(i -> new ReservaConfirmadaEvent.Item(i.getTipo().name(),
                        i.getDescripcion(), i.getPrecioCotizado())).toList(),
                r.getTotal(), r.getMoneda(), acompanantes));
    }

    public void pagoRechazado(PagoRechazadoEvent e) {
        Reserva r = reservas.findById(e.reservaId()).orElse(null);
        Participante p = e.participanteId() == null ? null : participantes.findById(e.participanteId()).orElse(null);
        if (r == null || p == null || r.getEstado() != EstadoReserva.PENDIENTE) return;

        tx.executeWithoutResult(s -> {
            p.marcarRechazado(e.motivo());
            participantes.save(p);
            saga.registrar(r, "PAGO", "FALLO", p.etiqueta() + ": " + e.motivo()
                    + (r.isCompartida() ? " (puede reintentar desde su link)" : ""));
        });
        if (!r.isCompartida()) {
            saga.compensar(r);
            cerrar(r, EstadoReserva.FALLIDA, "Pago rechazado: " + e.motivo());
        }
    }

    /** La invoca ReservasVencidasJob: nadie pago (o faltaron amigos) antes de vence_en. */
    public void expirar(Long id) {
        Reserva r = reservas.findById(id).orElse(null);
        if (r == null || r.getEstado() != EstadoReserva.PENDIENTE) return;
        String motivo = r.isCompartida()
                ? "No pagaron todos los participantes antes del vencimiento"
                : "No se recibio el pago a tiempo";
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
        saga.registrar(r, "CANCELAR", "OK", "Cancelada por el organizador");
        cerrar(r, EstadoReserva.CANCELADA, "Cancelada por el organizador");
        return obtener(id);
    }

    /** Estado final + reembolsos + devolucion de credito + evento, todo atomico (Outbox). */
    private void cerrar(Reserva r, EstadoReserva estado, String motivo) {
        Viajero v = viajeros.findById(r.getViajeroId()).orElseThrow();
        tx.executeWithoutResult(s -> {
            r.cambiarEstado(estado, motivo);
            reservas.save(r);
            List<Participante> todos = participantes.findByReservaIdOrderByIdAsc(r.getId());
            for (Participante p : todos) {
                if (p.getEstado() == EstadoParticipante.PAGADO) {
                    reembolsar(r, p, motivo);
                } else if (p.getEstado() != EstadoParticipante.REEMBOLSADO) {
                    p.cancelar();
                    participantes.save(p);
                }
            }
            creditos.devolver(r);
            List<String> acompanantes = todos.stream().filter(x -> !x.isOrganizador()).map(Participante::getEmail).toList();
            eventos.publicar(Eventos.RESERVA_CANCELADA, new ReservaCanceladaEvent(r.getId(), v.getEmail(),
                    v.getNombre(), estado.name(), motivo, acompanantes));
        });
    }

    private void reembolsar(Reserva r, Participante p, String motivo) {
        p.marcarReembolsado(motivo);
        participantes.save(p);
        if (!PAGO_CON_CREDITO.equals(p.getPagoRef())) {
            eventos.publicar(Eventos.REEMBOLSO_SOLICITADO, new ReembolsoSolicitadoEvent(r.getId(), p.getId(),
                    p.getEmail(), p.getMonto(), r.getMoneda(), motivo));
        }
        saga.registrar(r, "REEMBOLSO", "OK", "Se devuelven " + p.getMonto() + " " + r.getMoneda() + " a " + p.etiqueta());
    }

    // ------------------------------------------------------------------ consultas

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
        return ReservaDtos.Response.from(r, bitacora.findByReservaIdOrderByIdAsc(r.getId()),
                participantes.findByReservaIdOrderByIdAsc(r.getId()));
    }
}
