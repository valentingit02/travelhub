package com.travelhub.catalogo.oferta;

import com.travelhub.catalogo.auto.Auto;
import com.travelhub.catalogo.auto.AutoService;
import com.travelhub.catalogo.historial.HistorialPrecio;
import com.travelhub.catalogo.historial.HistorialPrecioRepository;
import com.travelhub.catalogo.mensajeria.EventPublisher;
import com.travelhub.catalogo.moneda.TipoCambioService;
import com.travelhub.catalogo.proveedor.OfertaProveedor;
import com.travelhub.common.domain.TipoProducto;
import com.travelhub.common.events.PrecioCambiadoEvent;
import com.travelhub.common.web.ConflictException;
import com.travelhub.common.web.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Registra cada oferta mostrada al usuario con su precio (en USD) y un vencimiento.
 * Al reservar, reservas-service consulta aca el precio: el navegador nunca lo define.
 * Cada oferta nueva o con precio distinto publica PrecioCambiado para que la IA la evalue.
 */
@Service
public class OfertaService {

    private static final Logger log = LoggerFactory.getLogger(OfertaService.class);

    private final OfertaPublicadaRepository repo;
    private final HistorialPrecioRepository historial;
    private final EventPublisher publisher;
    private final AutoService autos;
    private final TipoCambioService tipoCambio;
    private final Duration vigencia;

    public OfertaService(OfertaPublicadaRepository repo, HistorialPrecioRepository historial, EventPublisher publisher,
                         AutoService autos, TipoCambioService tipoCambio,
                         @Value("${travelhub.ofertas.vigencia:60m}") Duration vigencia) {
        this.repo = repo;
        this.historial = historial;
        this.publisher = publisher;
        this.autos = autos;
        this.tipoCambio = tipoCambio;
        this.vigencia = vigencia;
    }

    /** Registra las ofertas y devuelve solo las publicables (sin las bloqueadas por la IA). */
    public List<OfertaProveedor> publicar(List<OfertaProveedor> ofertas) {
        List<OfertaProveedor> visibles = new ArrayList<>();
        for (OfertaProveedor o : ofertas) {
            try {
                if (registrar(o)) visibles.add(o);
            } catch (DataIntegrityViolationException e) {
                visibles.add(o); // otra busqueda en paralelo la registro primero
            }
        }
        return visibles;
    }

    private boolean registrar(OfertaProveedor o) {
        Instant expira = Instant.now().plus(vigencia);
        Optional<OfertaPublicada> previa = repo.findById(o.id());
        if (previa.isEmpty()) {
            repo.save(new OfertaPublicada(o, expira));
            notificarPrecio(o, null);
            return true;
        }
        OfertaPublicada p = previa.get();
        if (p.isBloqueada()) return false;
        BigDecimal anterior = p.getPrecio();
        p.refrescar(o, expira);
        repo.save(p);
        if (anterior.compareTo(o.precioBase()) != 0) notificarPrecio(o, anterior);
        return true;
    }

    private void notificarPrecio(OfertaProveedor o, BigDecimal anterior) {
        historial.save(new HistorialPrecio(o.tipo().name(), o.id(), o.destino(), o.precioBase(), o.moneda()));
        publisher.precioCambiado(new PrecioCambiadoEvent(o.id(), o.tipo().name(), o.destino(), anterior,
                o.precioBase(), o.moneda()));
    }

    /** Precio y disponibilidad garantizados por el servidor para reservar. */
    @Transactional(readOnly = true)
    public OfertaVerificada verificar(String id, LocalDate desde, LocalDate hasta) {
        if (id.startsWith("AUTO-")) {
            Auto a = autos.verificarDisponible(parsearAuto(id), desde, hasta);
            return new OfertaVerificada(id, TipoProducto.AUTO, "TRAVELHUB",
                    a.getMarca() + " " + a.getModelo() + " (" + a.getCategoria() + ")", a.getDestinoIata(),
                    tipoCambio.aUsd(a.getPrecioBaseDia(), a.getMoneda()), TipoCambioService.MONEDA_BASE,
                    autos.ocupacion(a.getDestinoIata(), desde, hasta));
        }
        OfertaPublicada p = repo.findById(id).orElseThrow(() ->
                new ConflictException("La oferta ya no esta disponible. Volve a buscar para ver precios actualizados."));
        if (p.isBloqueada()) {
            throw new ConflictException("\"" + p.getNombre() + "\" esta en revision por un precio inusual y no se puede reservar.");
        }
        if (p.vencida()) {
            throw new ConflictException("El precio de \"" + p.getNombre() + "\" vencio. Volve a buscar para verlo actualizado.");
        }
        return new OfertaVerificada(p.getId(), p.getTipo(), p.getProveedor(), p.getNombre(), p.getDestino(),
                p.getPrecio(), p.getMoneda(), p.getOcupacion());
    }

    @Transactional
    public void bloquear(String id, String motivo) {
        repo.findById(id).ifPresentOrElse(p -> {
            p.bloquear(motivo);
            log.warn("Oferta {} BLOQUEADA por la IA: {}", id, motivo);
        }, () -> log.warn("Anomalia sobre una oferta desconocida: {}", id));
    }

    @Transactional
    public OfertaPublicada aprobar(String id) {
        OfertaPublicada p = repo.findById(id).orElseThrow(() -> new NotFoundException("No existe la oferta " + id));
        p.desbloquear();
        log.info("Oferta {} aprobada manualmente", id);
        return p;
    }

    @Transactional(readOnly = true)
    public List<OfertaPublicada> bloqueadas() {
        return repo.findByBloqueadaTrue();
    }

    private Long parsearAuto(String id) {
        try {
            return Long.valueOf(id.substring(5));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Id de auto invalido: " + id);
        }
    }
}
