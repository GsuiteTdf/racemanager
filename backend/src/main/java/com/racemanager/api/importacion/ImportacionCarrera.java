package com.racemanager.api.importacion;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Metadatos y resultado normalizado en revisión. Nunca publica resultados automáticamente. */
@Entity
@Table(name = "importacion_carrera", uniqueConstraints = {
    @UniqueConstraint(name = "uk_importacion_carrera_hash", columnNames = { "carrera_id", "hash_sha256" })
})
public class ImportacionCarrera {
    public enum Estado { PENDIENTE, PROCESADA, CONFIRMADA, RECHAZADA, ERROR }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "carrera_id", nullable = false)
    private Long carreraId;
    @Column(name = "manager_id", nullable = false)
    private Long managerId;
    @Column(name = "nombre_archivo", nullable = false, length = 255)
    private String nombreArchivo;
    @Column(name = "hash_sha256", nullable = false, length = 64)
    private String sha256;
    @Column(name = "tamano_bytes", nullable = false)
    private int tamanoBytes;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Estado estado = Estado.PENDIENTE;
    @Column(name = "procesado_en")
    private LocalDateTime procesadoEn;
    @Column(name = "detalle_json", columnDefinition = "LONGTEXT")
    private String detalleJson;

    protected ImportacionCarrera() { }

    public ImportacionCarrera(Long carreraId, Long managerId, String nombreArchivo,
                              String sha256, int tamanoBytes, String detalleJson) {
        this.carreraId = carreraId;
        this.managerId = managerId;
        this.nombreArchivo = nombreArchivo;
        this.sha256 = sha256;
        this.tamanoBytes = tamanoBytes;
        this.detalleJson = detalleJson;
        this.estado = Estado.PROCESADA;
        this.procesadoEn = LocalDateTime.now(java.time.Clock.systemUTC());
    }

    public Long getId() { return id; }
    public Long getCarreraId() { return carreraId; }
    public String getNombreArchivo() { return nombreArchivo; }
    public String getSha256() { return sha256; }
    public int getTamanoBytes() { return tamanoBytes; }
    public Estado getEstado() { return estado; }
    public LocalDateTime getProcesadoEn() { return procesadoEn; }
    public String getDetalleJson() { return detalleJson; }
}
