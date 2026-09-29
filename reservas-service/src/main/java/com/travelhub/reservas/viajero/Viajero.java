package com.travelhub.reservas.viajero;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "viajero")
public class Viajero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false, length = 80)
    private String apellido;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(nullable = false, length = 20)
    private String documento;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    /** Categorias de interes separadas por coma: aventura,gastronomia,cultura,naturaleza */
    @Column(length = 200)
    private String preferencias;

    protected Viajero() { }

    public Viajero(String nombre, String apellido, String email, String documento,
                   LocalDate fechaNacimiento, String preferencias) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.documento = documento;
        this.fechaNacimiento = fechaNacimiento;
        this.preferencias = preferencias;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getApellido() { return apellido; }
    public String getEmail() { return email; }
    public String getDocumento() { return documento; }
    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public String getPreferencias() { return preferencias; }
}
