package com.travelhub.reservas.saga;

import com.travelhub.reservas.cliente.CatalogoClient;
import com.travelhub.reservas.reserva.EstadoItem;
import com.travelhub.reservas.reserva.ItemReserva;
import com.travelhub.reservas.reserva.Reserva;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Saga orquestada: reserva cada item en su proveedor. Si uno falla, compensa
 * (cancela) en orden inverso los que ya se habian reservado.
 */
@Component
public class ReservaSaga {

    private static final Logger log = LoggerFactory.getLogger(ReservaSaga.class);

    private final CatalogoClient catalogo;
    private final SagaLogRepository bitacora;

    public ReservaSaga(CatalogoClient catalogo, SagaLogRepository bitacora) {
        this.catalogo = catalogo;
        this.bitacora = bitacora;
    }

    /** @return null si todo salio bien, o el motivo del fallo (ya compensado). */
    public String reservarItems(Reserva reserva, CatalogoClient.Titular titular) {
        for (ItemReserva item : reserva.getItems()) {
            String paso = "RESERVAR_" + item.getTipo();
            try {
                String ref = catalogo.reservar(item.getTipo(), item.getProductoId(), reserva.getDesde(),
                        reserva.getHasta(), reserva.getPasajeros(), reserva.referencia(), titular);
                item.marcarReservado(ref);
                registrar(reserva, paso, "OK", item.getDescripcion() + " -> " + ref);
            } catch (RuntimeException e) {
                item.marcarFallido();
                registrar(reserva, paso, "FALLO", e.getMessage());
                log.warn("Saga reserva {}: fallo {} ({}). Compensando...", reserva.getId(), paso, e.getMessage());
                compensar(reserva);
                return "Fallo al reservar " + item.getTipo() + ": " + e.getMessage();
            }
        }
        return null;
    }

    /** Cancela, en orden inverso, todos los items que estaban reservados. */
    public void compensar(Reserva reserva) {
        List<ItemReserva> reservados = new ArrayList<>(reserva.getItems().stream()
                .filter(i -> i.getEstado() == EstadoItem.RESERVADO).toList());
        Collections.reverse(reservados);
        for (ItemReserva item : reservados) {
            String paso = "COMPENSAR_" + item.getTipo();
            try {
                catalogo.cancelar(item.getTipo(), item.getRefExterna(), reserva.referencia());
                item.marcarCancelado();
                registrar(reserva, paso, "OK", "Cancelado " + item.getRefExterna());
            } catch (RuntimeException e) {
                // No se corta: se registra para revision manual y se sigue con el resto
                registrar(reserva, paso, "FALLO", e.getMessage());
                log.error("Saga reserva {}: no se pudo compensar {}: {}", reserva.getId(), item.getRefExterna(), e.getMessage());
            }
        }
    }

    public void registrar(Reserva reserva, String paso, String estado, String detalle) {
        bitacora.save(new SagaLog(reserva.getId(), paso, estado, detalle));
        log.info("Saga reserva {} | {} | {} | {}", reserva.getId(), paso, estado, detalle);
    }
}
