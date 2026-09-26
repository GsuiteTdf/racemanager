package com.racemanager.api.carrera;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Mapeo mínimo de carrera: solamente los campos requeridos por la carga de resultados. */
@Entity
@Table(name = "carrera")
public class Carrera {
    public enum Estado { PROGRAMADA, CARGADA, PUBLICADA, CANCELADA }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "liga_id", nullable = false)
    private Long ligaId;
    @Column(name = "circuito_id", nullable = false)
    private Long circuitoId;
    @Column(nullable = false, length = 150)
    private String nombre;
    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Estado estado = Estado.PROGRAMADA;

    protected Carrera() { }

    public Carrera(Long ligaId, Long circuitoId, String nombre, LocalDateTime fechaHora) {
        this.ligaId = ligaId;
        this.circuitoId = circuitoId;
        this.nombre = nombre;
        this.fechaHora = fechaHora;
    }

    public Long getId() { return id; }
    public Long getLigaId() { return ligaId; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
}
