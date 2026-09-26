package com.racemanager.api.importacion;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Adaptador para resultados JSON NATIVOS del servidor dedicado de Assetto Corsa original.
 * No es el JSON personalizado de Race Explorer ni el formato de Competizione.
 * Los puestos se devuelven como orden en el archivo, NO como clasificación oficial verificada.
 */
@Component
public class AcServerJsonParser implements AdaptadorResultados {
    private static final int MAX_PARTICIPANTES = 250;
    private static final int MAX_VUELTAS = 10_000;
    private final JsonMapper mapper;

    public AcServerJsonParser(JsonMapper mapper) { this.mapper = mapper; }

    @Override
    public ResultadoParseado parsear(byte[] contenido) {
        try {
            JsonNode raiz = mapper.readTree(contenido);
            if (raiz == null || !raiz.isObject()) {
                throw invalido("El archivo debe contener un objeto JSON");
            }
            String circuito = textoObligatorio(raiz, "TrackName", 150);
            String config = textoOpcional(raiz, "TrackConfig", 150);
            String tipo = textoObligatorio(raiz, "Type", 32);
            if (!Set.of("RACE", "QUALIFY", "PRACTICE").contains(tipo)) {
                throw invalido("Tipo de sesión no admitido: " + tipo);
            }
            int vueltasProgramadas = entero(raiz, "RaceLaps", 0, 10_000);
            JsonNode coches = arreglo(raiz, "Cars", MAX_PARTICIPANTES, false);
            JsonNode resultados = arreglo(raiz, "Result", MAX_PARTICIPANTES, false);
            JsonNode vueltas = arreglo(raiz, "Laps", MAX_VUELTAS, true);
            JsonNode eventos = raiz.get("Events");
            if (eventos != null && !eventos.isNull() && !eventos.isArray()) {
                throw invalido("Events debe ser un arreglo o null");
            }
            int cantidadEventos = eventos == null || eventos.isNull() ? 0 : eventos.size();
            if (coches.isEmpty() || resultados.isEmpty()) {
                throw invalido("El archivo debe contener coches y resultados");
            }

            Map<Integer, JsonNode> cochesPorId = new HashMap<>();
            for (JsonNode coche : coches) {
                if (!coche.isObject()) throw invalido("Cada elemento de Cars debe ser un objeto");
                int id = entero(coche, "CarId", 0, 1_000_000);
                if (cochesPorId.putIfAbsent(id, coche) != null) {
                    throw invalido("CarId duplicado en Cars: " + id);
                }
            }
            Map<Integer, List<ResultadoParseado.Vuelta>> vueltasPorCoche = new HashMap<>();
            List<String> advertencias = new ArrayList<>();
            for (JsonNode vuelta : vueltas) {
                if (!vuelta.isObject()) throw invalido("Cada vuelta debe ser un objeto");
                int carId = entero(vuelta, "CarId", 0, 1_000_000);
                if (!cochesPorId.containsKey(carId)) {
                    // Un log puede incluir vueltas de coches sin resultado final. No se atribuyen a otro piloto.
                    advertencias.add("Vuelta de un CarId sin coche registrado: " + carId);
                    continue;
                }
                long tiempo = entero(vuelta, "LapTime", 1, Integer.MAX_VALUE);
                int recortes = enteroOpcional(vuelta, "Cuts", 0, 10000);
                JsonNode sectores = vuelta.get("Sectors");
                List<Long> listaSectores = new ArrayList<>();
                if (sectores != null && !sectores.isNull()) {
                    if (!sectores.isArray() || sectores.size() > 3) {
                        throw invalido("Sectors debe contener como máximo tres tiempos");
                    }
                    for (JsonNode sector : sectores) {
                        if (!sector.isIntegralNumber()) throw invalido("Sector no numérico");
                        long valor = sector.longValue();
                        if (valor < -1 || valor > Integer.MAX_VALUE) throw invalido("Sector fuera de rango");
                        listaSectores.add(valor <= 0 ? null : valor);
                    }
                }
                List<ResultadoParseado.Vuelta> deCoche =
                    vueltasPorCoche.computeIfAbsent(carId, unused -> new ArrayList<>());
                deCoche.add(new ResultadoParseado.Vuelta(deCoche.size() + 1, tiempo,
                    List.copyOf(listaSectores.stream().map(x -> x == null ? -1L : x).toList()), recortes));
            }

            List<ResultadoParseado.Participante> participantes = new ArrayList<>();
            Set<Integer> resultadosRegistrados = new HashSet<>();
            int puesto = 0;
            for (JsonNode resultado : resultados) {
                if (!resultado.isObject()) throw invalido("Cada elemento de Result debe ser un objeto");
                int carId = entero(resultado, "CarId", 0, 1_000_000);
                if (!resultadosRegistrados.add(carId)) throw invalido("CarId repetido en Result: " + carId);
                JsonNode coche = cochesPorId.get(carId);
                if (coche == null) throw invalido("Result contiene un CarId ausente en Cars: " + carId);
                JsonNode conductor = coche.get("Driver");
                String nombre = textoOpcional(resultado, "DriverName", 120);
                if (nombre.isBlank() && conductor != null && conductor.isObject()) {
                    nombre = textoOpcional(conductor, "Name", 120);
                }
                if (nombre.isBlank()) throw invalido("Falta el nombre para CarId " + carId);
                String equipo = conductor != null && conductor.isObject()
                    ? textoOpcional(conductor, "Team", 120) : "";
                String modelo = textoObligatorio(coche, "Model", 150);
                List<ResultadoParseado.Vuelta> listaVueltas = vueltasPorCoche.getOrDefault(carId, List.of());
                participantes.add(new ResultadoParseado.Participante(++puesto, carId, nombre,
                    equipo, modelo, tiempoOpcional(resultado, "BestLap"),
                    tiempoOpcional(resultado, "TotalTime"), listaVueltas));
            }
            long vueltasSinResultado = vueltasPorCoche.entrySet().stream()
                .filter(e -> !resultadosRegistrados.contains(e.getKey()))
                .mapToLong(e -> e.getValue().size()).sum();
            if (vueltasSinResultado > 0) {
                advertencias.add(vueltasSinResultado + " vueltas sin participante en Result; se omitieron");
            }
            if (eventos == null || eventos.isNull()) {
                advertencias.add("El archivo no incluye eventos; no se infiere ausencia de incidentes");
            }
            advertencias.add("El orden de Result es provisional; el Manager debe revisar la clasificación antes de publicar");
            return new ResultadoParseado("AC_SERVER_JSON", circuito, config, tipo,
                vueltasProgramadas, vueltas.size(), cantidadEventos,
                List.copyOf(participantes), List.copyOf(advertencias));
        } catch (JacksonException ex) {
            throw invalido("El contenido no es un JSON válido del servidor de Assetto Corsa");
        }
    }

    private static JsonNode arreglo(JsonNode raiz, String clave, int max, boolean admiteVacio) {
        JsonNode nodo = raiz.get(clave);
        if (nodo == null || !nodo.isArray() || nodo.size() > max || (!admiteVacio && nodo.isEmpty())) {
            throw invalido(clave + " debe ser un arreglo no vacío, con un máximo de " + max + " elementos");
        }
        return nodo;
    }
    private static String textoObligatorio(JsonNode raiz, String clave, int max) {
        String valor = textoOpcional(raiz, clave, max);
        if (valor.isBlank()) throw invalido("Falta " + clave);
        return valor;
    }
    private static String textoOpcional(JsonNode raiz, String clave, int max) {
        JsonNode nodo = raiz.get(clave);
        if (nodo == null || nodo.isNull()) return "";
        if (!nodo.isTextual()) throw invalido(clave + " debe ser texto");
        String valor = nodo.stringValue().trim();
        if (valor.length() > max) throw invalido(clave + " es demasiado largo");
        return valor;
    }
    private static int entero(JsonNode raiz, String clave, int min, int max) {
        JsonNode nodo = raiz.get(clave);
        if (nodo == null || !nodo.isIntegralNumber() || !nodo.canConvertToInt()) {
            throw invalido(clave + " debe ser entero");
        }
        int valor = nodo.intValue();
        if (valor < min || valor > max) throw invalido(clave + " fuera de rango");
        return valor;
    }
    private static int enteroOpcional(JsonNode raiz, String clave, int min, int max) {
        JsonNode nodo = raiz.get(clave);
        return nodo == null || nodo.isNull() ? min : entero(raiz, clave, min, max);
    }
    private static Long tiempoOpcional(JsonNode raiz, String clave) {
        JsonNode nodo = raiz.get(clave);
        if (nodo == null || nodo.isNull()) return null;
        if (!nodo.isIntegralNumber()) throw invalido(clave + " debe ser numérico");
        long v = nodo.longValue();
        if (v < -1 || v > Integer.MAX_VALUE) throw invalido(clave + " fuera de rango");
        return v <= 0 ? null : v;
    }
    private static ArchivoImportacionInvalidoException invalido(String mensaje) {
        return new ArchivoImportacionInvalidoException(mensaje);
    }
}
