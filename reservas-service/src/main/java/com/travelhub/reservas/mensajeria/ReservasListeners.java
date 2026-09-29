package com.travelhub.reservas.mensajeria;

import com.travelhub.common.events.*;
import com.travelhub.reservas.credito.CreditoService;
import com.travelhub.reservas.reserva.ReservaFacade;
import com.travelhub.reservas.seguimiento.SeguimientoRepository;
import com.travelhub.reservas.viajero.ViajeroRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ReservasListeners {

    private static final Logger log = LoggerFactory.getLogger(ReservasListeners.class);

    private final ReservaFacade facade;
    private final CreditoService creditos;
    private final SeguimientoRepository seguimientos;
    private final ViajeroRepository viajeros;
    private final ReservasEventPublisher eventos;

    public ReservasListeners(ReservaFacade facade, CreditoService creditos, SeguimientoRepository seguimientos,
                             ViajeroRepository viajeros, ReservasEventPublisher eventos) {
        this.facade = facade;
        this.creditos = creditos;
        this.seguimientos = seguimientos;
        this.viajeros = viajeros;
        this.eventos = eventos;
    }

    @RabbitListener(queues = ReservasRabbitConfig.COLA_PAGO_APROBADO)
    public void onPagoAprobado(PagoAprobadoEvent e) {
        log.info("Recibido PagoAprobado reserva={} parte={}", e.reservaId(), e.participanteId());
        facade.pagoAprobado(e);
    }

    @RabbitListener(queues = ReservasRabbitConfig.COLA_PAGO_RECHAZADO)
    public void onPagoRechazado(PagoRechazadoEvent e) {
        log.info("Recibido PagoRechazado reserva={} parte={} motivo={}", e.reservaId(), e.participanteId(), e.motivo());
        facade.pagoRechazado(e);
    }

    /**
     * Un cambio de precio dispara dos cosas independientes:
     *  1. Proteccion de precio: credito a quienes ya reservaron ese producto mas caro.
     *  2. Alertas: aviso a quienes siguen el destino con un precio objetivo.
     */
    @RabbitListener(queues = ReservasRabbitConfig.COLA_PRECIO_CAMBIADO)
    public void onPrecioCambiado(PrecioCambiadoEvent e) {
        try {
            int n = creditos.protegerPrecio(e);
            if (n > 0) log.info("Proteccion de precio: {} reserva(s) recibieron credito por {}", n, e.productoRef());
        } catch (RuntimeException ex) {
            log.error("Fallo la proteccion de precio para {}: {}", e.productoRef(), ex.getMessage());
        }
        seguimientos.findByDestinoIgnoreCaseAndTipoProductoAndActivoTrue(e.destino(), e.tipoProducto()).stream()
                .filter(s -> s.getMoneda().equalsIgnoreCase(e.moneda()))
                .filter(s -> e.precioNuevo().compareTo(s.getPrecioObjetivo()) <= 0)
                .forEach(s -> viajeros.findById(s.getViajeroId()).ifPresent(v ->
                        eventos.publicar(Eventos.ALERTA_PRECIO, new AlertaPrecioEvent(v.getEmail(), v.getNombre(),
                                e.destino(), e.productoRef(), e.precioNuevo(), s.getPrecioObjetivo(), e.moneda()))));
    }
}
