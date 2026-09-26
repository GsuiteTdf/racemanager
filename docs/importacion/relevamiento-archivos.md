# Relevamiento de archivos de Assetto Corsa

**Primera modalidad seleccionada:** JSON nativo del **servidor dedicado de Assetto Corsa original**.
La etapa inicial de lectura y previsualización está documentada en
[`importador-ac-servidor.md`](importador-ac-servidor.md).

## Ejemplos públicos examinados

| Caso | Fuente | Formato | Particularidad |
|---|---|---|---|
| Carrera Monza 2015 | [Simresults / prueba servidor](https://github.com/mauserrifle/simresults/blob/develop/tests/logs/assettocorsa-server-json/2015_10_17_9_49_RACE.json) | JSON nativo | `Cars`, `Result`, `Laps`, `Events`; 17 coches, 82 vueltas |
| Clasificación Monza 2015 | [Simresults / prueba servidor](https://github.com/mauserrifle/simresults/blob/develop/tests/logs/assettocorsa-server-json/2015_10_17_9_30_QUALIFY.json) | JSON nativo | Clasificación y tiempos por vuelta |
| Eventos ausentes | [Simresults / prueba de robustez](https://github.com/mauserrifle/simresults/blob/develop/tests/logs/assettocorsa-server-json/race.changed.with.null.events.json) | JSON nativo | `Events: null` |
| Carrera personal 2026 | [Race Explorer, ejemplo](https://github.com/pecamardelli/Assetto-Corsa-Race-Explorer) | JSON **personalizado** | `session_info`, `driver_statistics`; segunda modalidad futura |

Para el repositorio se redactó **una muestra sintética anonimizada**:
`backend/src/test/resources/ejemplos/ac-servidor-carrera.json`.

## Validación pendiente con la liga real

Antes de confirmar y publicar resultados oficiales todavía debemos obtener al menos dos archivos
recientes: una carrera completa y un caso con abandono, desconexión o vueltas inválidas. Deben
anonimizarse nombres e identificadores externos antes de versionarlos. Hay que comprobar
especialmente el orden de `Result` y cómo distinguir posiciones oficiales de otros criterios
(como la mejor vuelta) para no crear una clasificación incorrecta.

**No mezclar formatos:** los ejemplos XML/TXT de la web de Simresults no son, por sí solos,
archivos JSON nativos de Assetto Corsa. Los resultados de Competizione también requieren un
adaptador diferente y pueden usar UTF-16.
