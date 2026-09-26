package com.racemanager.api.importacion;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.racemanager.api.auth.UsuarioAutenticado;
import com.racemanager.api.importacion.ImportacionCarreraService.ImportacionVista;

/** Importación a estado PROCESADA para revisión; no confirma ni publica. */
@RestController
@RequestMapping("/api/ligas/{ligaId}/carreras/{carreraId}/importaciones")
public class ImportacionCarreraController {
    private final ImportacionCarreraService servicio;

    public ImportacionCarreraController(ImportacionCarreraService servicio) {
        this.servicio = servicio;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@ligaAccess.puedeGestionar(authentication, #ligaId)")
    public ImportacionVista cargar(@PathVariable("ligaId") @P("ligaId") Long ligaId,
            @PathVariable("carreraId") Long carreraId,
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestPart("archivo") MultipartFile archivo) {
        return servicio.cargar(ligaId, carreraId, usuario.id(), archivo);
    }

    @GetMapping("/{importacionId}")
    @PreAuthorize("@ligaAccess.puedeGestionar(authentication, #ligaId)")
    public ImportacionVista obtener(@PathVariable("ligaId") @P("ligaId") Long ligaId,
            @PathVariable("carreraId") Long carreraId,
            @PathVariable("importacionId") Long importacionId) {
        return servicio.obtener(ligaId, carreraId, importacionId);
    }
}
