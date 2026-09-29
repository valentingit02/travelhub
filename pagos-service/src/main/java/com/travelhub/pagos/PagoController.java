package com.travelhub.pagos;

import com.travelhub.common.web.NotFoundException;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pagos")
@Tag(name = "Pagos")
public class PagoController {

    private final PagoRepository repo;

    public PagoController(PagoRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Pago> listar(@RequestParam(required = false) Long reservaId) {
        if (reservaId != null) return repo.findByReservaId(reservaId).stream().toList();
        return repo.findAll();
    }

    @GetMapping("/{id}")
    public Pago obtener(@PathVariable Long id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("No existe el pago " + id));
    }
}
