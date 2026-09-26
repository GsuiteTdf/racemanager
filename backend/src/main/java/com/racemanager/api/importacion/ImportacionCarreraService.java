package com.racemanager.api.importacion;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.racemanager.api.carrera.Carrera;
import com.racemanager.api.carrera.CarreraRepository;
import com.racemanager.api.common.RecursoNoEncontradoException;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Service
public class ImportacionCarreraService {
    public static final int MAX_BYTES = 2 * 1024 * 1024;

    public record ImportacionVista(
        Long id, Long carreraId, String nombreArchivo, String sha256, int tamanoBytes,
        String estado, ResultadoParseado resultado) { }

    private final CarreraRepository carreras;
    private final ImportacionCarreraRepository importaciones;
    private final AcServerJsonParser parser;
    private final JsonMapper mapper;

    public ImportacionCarreraService(CarreraRepository carreras,
            ImportacionCarreraRepository importaciones, AcServerJsonParser parser, JsonMapper mapper) {
        this.carreras = carreras;
        this.importaciones = importaciones;
        this.parser = parser;
        this.mapper = mapper;
    }

    @Transactional
    public ImportacionVista cargar(Long ligaId, Long carreraId, Long usuarioId, MultipartFile archivo) {
        Carrera carrera = validarCarrera(ligaId, carreraId);
        if (carrera.getEstado() == Carrera.Estado.PUBLICADA ||
                carrera.getEstado() == Carrera.Estado.CANCELADA) {
            throw new ImportacionNoPermitidaException("La carrera no admite nuevas importaciones en su estado actual");
        }
        if (archivo == null || archivo.isEmpty()) {
            throw new ArchivoImportacionInvalidoException("Seleccioná un archivo JSON no vacío");
        }
        if (archivo.getSize() > MAX_BYTES) {
            throw new ArchivoImportacionInvalidoException("El archivo supera el límite de 2 MB");
        }
        String nombre = limpiarNombre(archivo.getOriginalFilename());
        byte[] contenido;
        try {
            contenido = archivo.getBytes();
        } catch (IOException ex) {
            throw new ArchivoImportacionInvalidoException("No se pudo leer el archivo");
        }
        if (contenido.length == 0 || contenido.length > MAX_BYTES) {
            throw new ArchivoImportacionInvalidoException("El archivo debe tener entre 1 byte y 2 MB");
        }
        String sha = sha256(contenido);
        if (importaciones.existsByCarreraIdAndSha256(carreraId, sha)) {
            throw new ImportacionDuplicadaException();
        }
        ResultadoParseado resultado = parser.parsear(contenido);
        String detalle = mapper.writeValueAsString(resultado);
        ImportacionCarrera entidad = new ImportacionCarrera(carreraId, usuarioId, nombre,
            sha, contenido.length, detalle);
        try {
            entidad = importaciones.saveAndFlush(entidad);
        } catch (DataIntegrityViolationException ex) {
            // La restricción UNIQUE también protege de subidas concurrentes del mismo archivo.
            throw new ImportacionDuplicadaException();
        }
        return vista(entidad, resultado);
    }

    @Transactional(readOnly = true)
    public ImportacionVista obtener(Long ligaId, Long carreraId, Long importacionId) {
        validarCarrera(ligaId, carreraId);
        ImportacionCarrera entidad = importaciones.findByIdAndCarreraId(importacionId, carreraId)
            .orElseThrow(() -> new RecursoNoEncontradoException("Importación no encontrada"));
        try {
            ResultadoParseado resultado = mapper.readValue(entidad.getDetalleJson(), ResultadoParseado.class);
            return vista(entidad, resultado);
        } catch (JacksonException ex) {
            throw new IllegalStateException("Detalle de importación ilegible", ex);
        }
    }

    private Carrera validarCarrera(Long ligaId, Long carreraId) {
        Carrera carrera = carreras.findById(carreraId)
            .orElseThrow(() -> new RecursoNoEncontradoException("Carrera no encontrada"));
        if (!carrera.getLigaId().equals(ligaId)) {
            // No revelar si existe una carrera de otra liga a quien solo conoce este ligaId.
            throw new AccessDeniedException("La carrera no pertenece a la liga indicada");
        }
        return carrera;
    }

    private static String limpiarNombre(String nombre) {
        if (nombre == null) throw new ArchivoImportacionInvalidoException("Falta el nombre del archivo");
        String limpio = nombre.replace('\\', '/');
        limpio = limpio.substring(limpio.lastIndexOf('/') + 1);
        if (limpio.isBlank() || limpio.length() > 255 ||
            limpio.chars().anyMatch(c -> Character.isISOControl((char) c)) ||
            !limpio.toLowerCase(Locale.ROOT).endsWith(".json")) {
            throw new ArchivoImportacionInvalidoException("El nombre debe terminar en .json y no contener caracteres inválidos");
        }
        return limpio;
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException imposible) {
            throw new IllegalStateException("SHA-256 no disponible", imposible);
        }
    }

    private static ImportacionVista vista(ImportacionCarrera entidad, ResultadoParseado resultado) {
        return new ImportacionVista(entidad.getId(), entidad.getCarreraId(), entidad.getNombreArchivo(),
            entidad.getSha256(), entidad.getTamanoBytes(), entidad.getEstado().name(), resultado);
    }
}
