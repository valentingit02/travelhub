package com.travelhub.reservas.credito;

import com.travelhub.common.events.CreditoOtorgadoEvent;
import com.travelhub.common.events.Eventos;
import com.travelhub.common.events.PrecioCambiadoEvent;
import com.travelhub.reservas.mensajeria.ReservasEventPublisher;
import com.travelhub.reservas.reserva.EstadoReserva;
import com.travelhub.reservas.reserva.ItemReserva;
import com.travelhub.reservas.reserva.Reserva;
import com.travelhub.reservas.reserva.ReservaRepository;
import com.travelhub.reservas.viajero.ViajeroRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * Credito del viajero y Proteccion de precio: si despues de reservar baja el precio de algo
 * incluido en un viaje confirmado (y futuro), se devuelve la diferencia como credito, que se
 * aplica solo en la proxima reserva individual.
 */
@Service
public class CreditoService {

    private static final Logger log = LoggerFactory.getLogger(CreditoService.class);
    /** Bajas de mas del 50 % se tratan como posible error de carga: no se compensan solas. */
    static final BigDecimal BAJA_MAXIMA = new BigDecimal("0.5");

    private final MovimientoCreditoRepository repo;
    private final ReservaRepository reservas;
    private final ViajeroRepository viajeros;
    private final ReservasEventPublisher eventos;
    private final TransactionTemplate tx;

    public CreditoService(MovimientoCreditoRepository repo, ReservaRepository reservas, ViajeroRepository viajeros,
                          ReservasEventPublisher eventos, TransactionTemplate tx) {
        this.repo = repo;
        this.reservas = reservas;
        this.viajeros = viajeros;
        this.eventos = eventos;
        this.tx = tx;
    }

    public BigDecimal saldo(Long viajeroId) {
        return repo.findByViajeroIdOrderByIdDesc(viajeroId).stream()
                .map(MovimientoCredito::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<MovimientoCredito> movimientos(Long viajeroId) {
        return repo.findByViajeroIdOrderByIdDesc(viajeroId);
    }

    /** Usa el saldo disponible para pagar (parte de) la reserva. Devuelve el monto aplicado. */
    @Transactional
    public BigDecimal aplicar(Long viajeroId, Long reservaId, BigDecimal total, String moneda) {
        BigDecimal disponible = saldo(viajeroId);
        if (disponible.signum() <= 0 || total.signum() <= 0) return BigDecimal.ZERO;
        BigDecimal monto = disponible.min(total);
        repo.save(new MovimientoCredito(viajeroId, reservaId, null, monto.negate(), moneda,
                MovimientoCredito.Tipo.USO, "Aplicado a la reserva #" + reservaId));
        return monto;
    }

    /** Si la reserva se cancela o falla, el credito usado vuelve al viajero. */
    @Transactional
    public void devolver(Reserva r) {
        if (r.getCreditoAplicado() == null || r.getCreditoAplicado().signum() <= 0) return;
        repo.save(new MovimientoCredito(r.getViajeroId(), r.getId(), null, r.getCreditoAplicado(), r.getMoneda(),
                MovimientoCredito.Tipo.DEVOLUCION, "Devolucion por la reserva #" + r.getId() + " " + r.getEstado()));
    }

    /** Consumidor de PrecioCambiado. Devuelve cuantas reservas recibieron credito. */
    public int protegerPrecio(PrecioCambiadoEvent e) {
        if (e.productoRef() == null || e.precioNuevo() == null) return 0;
        int n = 0;
        for (Reserva r : reservas.conProducto(e.productoRef(), EstadoReserva.CONFIRMADA, LocalDate.now())) {
            Boolean ok = tx.execute(s -> compensar(r, e));
            if (Boolean.TRUE.equals(ok)) n++;
        }
        return n;
    }

    private boolean compensar(Reserva r, PrecioCambiadoEvent e) {
        for (ItemReserva item : r.getItems()) {
            if (!item.getProductoId().equals(e.productoRef())) continue;
            if (e.moneda() != null && r.getMoneda() != null && !e.moneda().equalsIgnoreCase(r.getMoneda())) continue;
            BigDecimal referencia = item.getPrecioProtegido() != null ? item.getPrecioProtegido() : item.getPrecioBase();
            BigDecimal monto = compensacion(item.getPrecioCotizado(), item.getPrecioBase(), referencia, e.precioNuevo());
            if (monto.signum() <= 0) {
                if (e.precioNuevo().compareTo(item.getPrecioBase().multiply(BAJA_MAXIMA)) < 0) {
                    log.warn("Baja de precio de {} demasiado grande ({} -> {}): posible error, no se compensa",
                            e.productoRef(), item.getPrecioBase(), e.precioNuevo());
                }
                continue;
            }
            item.setPrecioProtegido(e.precioNuevo());
            reservas.save(r);
            repo.save(new MovimientoCredito(r.getViajeroId(), r.getId(), item.getId(), monto, r.getMoneda(),
                    MovimientoCredito.Tipo.OTORGADO, "Bajo el precio de " + item.getDescripcion()));
            BigDecimal saldo = saldo(r.getViajeroId());
            viajeros.findById(r.getViajeroId()).ifPresent(v -> eventos.publicar(Eventos.CREDITO_OTORGADO,
                    new CreditoOtorgadoEvent(v.getId(), v.getEmail(), v.getNombre(), r.getId(), item.getDescripcion(),
                            monto, saldo, r.getMoneda())));
            log.info("Proteccion de precio: reserva {} recibe {} {} por {}", r.getId(), monto, r.getMoneda(),
                    item.getDescripcion());
            return true;
        }
        return false;
    }

    /**
     * Credito = lo que se pago por el item x la baja relativa del precio base.
     * Asi respeta temporada, anticipacion y descuento de paquete que ya tenia el precio pagado.
     */
    public static BigDecimal compensacion(BigDecimal pagado, BigDecimal base, BigDecimal referencia, BigDecimal nuevo) {
        if (pagado == null || base == null || base.signum() <= 0 || nuevo == null) return BigDecimal.ZERO;
        if (nuevo.compareTo(referencia) >= 0) return BigDecimal.ZERO;
        if (nuevo.compareTo(base.multiply(BAJA_MAXIMA)) < 0) return BigDecimal.ZERO;
        return pagado.multiply(referencia.subtract(nuevo)).divide(base, 2, RoundingMode.HALF_UP);
    }
}
