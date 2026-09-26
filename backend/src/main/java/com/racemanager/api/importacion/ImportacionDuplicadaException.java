package com.racemanager.api.importacion;

public class ImportacionDuplicadaException extends RuntimeException {
    public ImportacionDuplicadaException() {
        super("El mismo archivo ya fue cargado para esta carrera");
    }
}
