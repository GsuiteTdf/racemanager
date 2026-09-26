package com.racemanager.api.importacion;

/** Punto de extensión para fuentes futuras; la primera implementación procesa JSON nativo AC. */
public interface AdaptadorResultados {
    ResultadoParseado parsear(byte[] contenido);
}
