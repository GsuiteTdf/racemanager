package com.racemanager.api.importacion;

public class ImportacionNoPermitidaException extends RuntimeException {
    public ImportacionNoPermitidaException(String mensaje) { super(mensaje); }
}
