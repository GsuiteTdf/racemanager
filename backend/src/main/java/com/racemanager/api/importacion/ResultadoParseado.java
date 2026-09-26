package com.racemanager.api.importacion;

import java.util.List;

/** Contrato normalizado: se reutilizará con un adaptador Race Explorer, sin copiar su código. */
public record ResultadoParseado(
    String fuente, String circuito, String configuracionCircuito, String tipoSesion,
    int vueltasProgramadas, int vueltasRegistradas, int eventosRegistrados,
    List<Participante> participantes, List<String> advertencias) {

    public record Participante(
        int ordenEnArchivo, int carId, String nombre, String equipo, String modeloVehiculo,
        Long mejorVueltaMs, Long tiempoTotalMs, List<Vuelta> vueltas) { }

    public record Vuelta(int numero, long tiempoMs, List<Long> sectoresMs, int cortes) { }
}
