package com.travelhub.reservas.viajero;

import jakarta.persistence.*;
import java.time.Instant;
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

    /** Hash BCrypt. Null en cuentas creadas antes del login: se completa con /api/auth/activar. */
    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Column(length = 30)
    private String telefono;

    /** Aeropuerto desde el que suele salir (IATA): precarga el buscador. */
    @Column(name = "aeropuerto_origen", length = 3)
    private String aeropuertoOrigen;

    @Column(name = "ultimo_ingreso")
    private Instant ultimoIngreso;

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

    public void actualizarPerfil(String nombre, String apellido, String documento, LocalDate fechaNacimiento,
                                 String preferencias, String telefono, String aeropuertoOrigen) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.documento = documento;
        this.fechaNacimiento = fechaNacimiento;
        this.preferencias = preferencias;
        this.telefono = telefono;
        this.aeropuertoOrigen = aeropuertoOrigen;
    }

    public void cambiarPassword(String nuevoHash) { this.passwordHash = nuevoHash; }
    public void registrarIngreso() { this.ultimoIngreso = Instant.now(); }
    public boolean tienePassword() { return passwordHash != null && !passwordHash.isBlank(); }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getApellido() { return apellido; }
    public String getEmail() { return email; }
    public String getDocumento() { return documento; }
    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public String getPreferencias() { return preferencias; }
    public String getPasswordHash() { return passwordHash; }
    public String getTelefono() { return telefono; }
    public String getAeropuertoOrigen() { return aeropuertoOrigen; }
    public Instant getUltimoIngreso() { return ultimoIngreso; }
}
