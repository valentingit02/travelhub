package com.travelhub.catalogo.auto;

import com.travelhub.catalogo.historial.HistorialPrecio;
import com.travelhub.catalogo.historial.HistorialPrecioRepository;
import com.travelhub.catalogo.mensajeria.EventPublisher;
import com.travelhub.common.domain.TipoProducto;
import com.travelhub.common.events.PrecioCambiadoEvent;
import com.travelhub.common.util.ValidadorFechas;
import com.travelhub.common.web.ConflictException;
import com.travelhub.common.web.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Capa de logica de negocio del inventario propio de autos. */
@Service
@Transactional
public class AutoService {

    private static final Logger log = LoggerFactory.getLogger(AutoService.class);

    private final AutoRepository repository;
    private final BloqueoAutoRepository bloqueos;
    private final HistorialPrecioRepository historial;
    private final EventPublisher publisher;

    public AutoService(AutoRepository repository, BloqueoAutoRepository bloqueos,
                       HistorialPrecioRepository historial, EventPublisher publisher) {
        this.repository = repository;
        this.bloqueos = bloqueos;
        this.historial = historial;
        this.publisher = publisher;
    }

    @Transactional(readOnly = true)
    public List<AutoResponse> listar(String destino) {
        List<Auto> autos = (destino == null || destino.isBlank())
                ? repository.findByActivoTrue()
                : repository.findByDestinoIataIgnoreCaseAndActivoTrueAndEnRevisionFalse(destino);
        return autos.stream().map(AutoResponse::from).toList();
    }

    /** Autos publicables del destino que no estan reservados en el rango. */
    @Transactional(readOnly = true)
    public List<Auto> disponibles(String destino, LocalDate desde, LocalDate hasta) {
        return repository.findByDestinoIataIgnoreCaseAndActivoTrueAndEnRevisionFalse(destino).stream()
                .filter(a -> bloqueos.contarSuperpuestos(a.getId(), desde, hasta) == 0)
                .toList();
    }

    /** Ocupacion del destino en el rango: fraccion de autos ya reservados (0.0 a 1.0). */
    @Transactional(readOnly = true)
    public double ocupacion(String destino, LocalDate desde, LocalDate hasta) {
        List<Auto> todos = repository.findByDestinoIataIgnoreCaseAndActivoTrueAndEnRevisionFalse(destino);
        if (todos.isEmpty()) return 0.0;
        long ocupados = todos.stream().filter(a -> bloqueos.contarSuperpuestos(a.getId(), desde, hasta) > 0).count();
        return (double) ocupados / todos.size();
    }

    /** Verifica que el auto se pueda reservar en esas fechas (lo usa la verificacion de ofertas). */
    @Transactional(readOnly = true)
    public Auto verificarDisponible(Long id, LocalDate desde, LocalDate hasta) {
        Auto a = buscar(id);
        if (!a.isActivo() || a.isEnRevision()) {
            throw new ConflictException("El auto " + a.getMarca() + " " + a.getModelo() + " no esta disponible para reservar");
        }
        if (bloqueos.contarSuperpuestos(id, desde, hasta) > 0) {
            throw new ConflictException("El auto " + a.getMarca() + " " + a.getModelo() + " ya esta reservado en esas fechas");
        }
        return a;
    }

    @Transactional(readOnly = true)
    public AutoResponse obtener(Long id) {
        return AutoResponse.from(buscar(id));
    }

    public AutoResponse crear(AutoRequest r) {
        Auto auto = new Auto(r.destinoIata().toUpperCase(), r.categoria(), r.marca(), r.modelo(), r.plazas(),
                r.transmisionAutomatica(), r.precioBaseDia(), r.moneda().toUpperCase());
        Auto guardado = repository.save(auto);
        log.info("Auto creado id={} destino={}", guardado.getId(), guardado.getDestinoIata());
        registrarPrecio(guardado, null);
        return AutoResponse.from(guardado);
    }

    public AutoResponse actualizar(Long id, AutoRequest r) {
        Auto auto = buscar(id);
        BigDecimal precioAnterior = auto.getPrecioBaseDia();
        auto.actualizar(r);
        log.info("Auto actualizado id={}", id);
        if (precioAnterior.compareTo(auto.getPrecioBaseDia()) != 0) {
            registrarPrecio(auto, precioAnterior);
        }
        return AutoResponse.from(auto);
    }

    public void desactivar(Long id) {
        buscar(id).desactivar();
        log.info("Auto desactivado id={}", id);
    }

    public void marcarEnRevision(Long id) {
        repository.findById(id).ifPresent(a -> {
            a.marcarEnRevision();
            log.warn("Auto id={} marcado EN REVISION por precio anomalo", id);
        });
    }

    public AutoResponse aprobarRevision(Long id) {
        Auto a = buscar(id);
        a.aprobarRevision();
        log.info("Auto id={} aprobado manualmente, vuelve a publicarse", id);
        return AutoResponse.from(a);
    }

    public BloqueoAuto bloquear(Long autoId, LocalDate desde, LocalDate hasta, String reservaRef) {
        ValidadorFechas.validarRango(desde, hasta);
        verificarDisponible(autoId, desde, hasta);
        BloqueoAuto b = bloqueos.save(new BloqueoAuto(autoId, desde, hasta, reservaRef));
        log.info("Auto {} bloqueado {} a {} para {}", autoId, desde, hasta, reservaRef);
        return b;
    }

    public int liberarBloqueos(String reservaRef) {
        return bloqueos.liberarPorReserva(reservaRef);
    }

    private void registrarPrecio(Auto a, BigDecimal anterior) {
        String ref = "AUTO-" + a.getId();
        historial.save(new HistorialPrecio(TipoProducto.AUTO.name(), ref, a.getDestinoIata(),
                a.getPrecioBaseDia(), a.getMoneda()));
        publisher.precioCambiado(new PrecioCambiadoEvent(ref, TipoProducto.AUTO.name(), a.getDestinoIata(),
                anterior, a.getPrecioBaseDia(), a.getMoneda()));
    }

    private Auto buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("No existe el auto con id " + id));
    }
}
