# Importador V1 — JSON nativo del servidor dedicado de Assetto Corsa

**Estado:** primera etapa de carga y previsualización implementada en el backend. **No** confirma
pilotos, no crea carreras, no escribe todavía en `sesion`, `resultado_sesion` o `vuelta` y **no
publica** clasificaciones. Estas operaciones requieren la iteración siguiente (mapeo de pilotos,
revisión humana y confirmación transaccional).

## Formato elegido y referencia verificable

El lector acepta **JSON del servidor dedicado de Assetto Corsa original**, no Assetto Corsa
Competizione. Se estudiaron las muestras históricas utilizadas en Simresults:

- [Carrera de Monza: 2015_10_17_9_49_RACE.json](https://github.com/mauserrifle/simresults/blob/develop/tests/logs/assettocorsa-server-json/2015_10_17_9_49_RACE.json)
- [Clasificación: 2015_10_17_9_30_QUALIFY.json](https://github.com/mauserrifle/simresults/blob/develop/tests/logs/assettocorsa-server-json/2015_10_17_9_30_QUALIFY.json)
- [Caso especial: Events = null](https://github.com/mauserrifle/simresults/blob/develop/tests/logs/assettocorsa-server-json/race.changed.with.null.events.json)

El archivo sintético **`backend/src/test/resources/ejemplos/ac-servidor-carrera.json`** respeta los
campos observados, pero contiene nombres e identificadores ficticios. No se incorporaron al
repositorio datos personales ni Steam IDs de los archivos públicos.

| JSON original | Uso en RaceManager |
|---|---|
| `TrackName`, `TrackConfig`, `Type`, `RaceLaps` | Metadatos de la sesión |
| `Cars[*].CarId`, `Driver.Name`, `Driver.Team`, `Model` | Referencias locales de pilotos y vehículos |
| `Result[*].DriverName`, `CarId`, `BestLap`, `TotalTime` | Vista previa ordenada tal como llegó |
| `Laps[*].CarId`, `LapTime`, `Sectors`, `Cuts` | Vueltas, tiempos en **milisegundos**, sectores y cortes |
| `Events` | Cantidad de eventos; no se almacenan detalles de incidentes en V1 |

Los datos externos `Driver.Guid` y `DriverGuid` se **descartan** en V1. `CarId` identifica un
vehículo **dentro de ese archivo**, no a un usuario estable entre carreras.

**Importante:** `ordenEnArchivo` es un orden provisional. Sin verificar en una liga real la regla de
ordenamiento de `Result` para cada tipo de sesión, no corresponde presentarlo como posición
oficial, ni usarlo para adjudicar puntos.

## API protegida

Se requiere un JWT válido de un `MANAGER` de la liga o un `ADMIN`. Además del control de liga,
el servicio verifica que la carrera indicada pertenece a esa liga. Antes de probar la API hay
que disponer de una carrera registrada en la base; el endpoint de alta de carreras aún no existe.

### Subir un archivo JSON (multipart)

```
POST /api/ligas/{ligaId}/carreras/{carreraId}/importaciones
Authorization: Bearer <token>
Content-Type: multipart/form-data
Campo: archivo (archivo.json)
```

Desde PowerShell con `curl.exe` (reemplazar todos los valores de ejemplo):

```powershell
curl.exe -X POST `
  -H "Authorization: Bearer $env:RACEMANAGER_TOKEN" `
  -F "archivo=@C:\ruta\resultado.json;type=application/json" `
  http://localhost:8080/api/ligas/1/carreras/1/importaciones
```

Respuesta: `201 Created` con `id`, `sha256`, `estado: PROCESADA` y un objeto `resultado` que
contiene circuito, sesión, participantes, vueltas y advertencias.

### Recuperar la previsualización almacenada

```
GET /api/ligas/{ligaId}/carreras/{carreraId}/importaciones/{importacionId}
Authorization: Bearer <token>
```

El resumen normalizado se guarda en `importacion_carrera.detalle_json` (agregado por la migración
003). No se guarda el archivo original ni sus GUID. La vista previa sigue disponible tras
reiniciar el backend.

### Validaciones implementadas

- Solo `.json`, contenido no vacío y límite de **2 MiB** (también impuesto a nivel HTTP).
- JSON bien formado, tipo `RACE`, `QUALIFY` o `PRACTICE`; `Cars`/`Result` no vacíos.
- Identificadores `CarId` únicos, correspondencia de resultados con vehículos y tiempos en rango.
- Las vueltas se asocian por `CarId`. Los eventos nulos se admiten con advertencia.
- SHA-256 para impedir el mismo archivo dos veces en **la misma carrera** (`409`).
- El Manager no puede cargar o leer importaciones de otra liga (`403`).
- No se importan nuevas muestras en carreras publicadas o canceladas (`409`).
- La carga **no cambia** automáticamente el estado de la carrera a `CARGADA` ni publica resultados.

## Migración de bases ya existentes

Aplicar **una vez**, después de la 002 si fuera necesaria:

```bash
mysql -u root -p < database/migraciones/003_detalle_importacion.sql
```

Una base nueva creada con `database/schema.sql` ya incluye la columna. El CI prueba tanto una
base nueva como una base v1 con migraciones 002 + 003. No aplicar manualmente las migraciones
sobre la base de un compañero sin respaldo.

## Segunda modalidad: Race Explorer (futuro)

El proyecto de Diego, [Assetto Corsa Race Explorer](https://github.com/pecamardelli/Assetto-Corsa-Race-Explorer),
usa un módulo propio `scripts/racestats.py` que captura datos **durante la partida** y escribe
archivos `stats_*.json`, con una estructura **diferente**. Su archivo de ejemplo ofrece
`session_info` (`track`, `race_laps`, `session_type`, `finished`) y `driver_statistics` con
posiciones, vueltas, accidentes y puntuaciones calculadas por la aplicación. No es el JSON
nativo del servidor dedicado.

El punto de extensión `AdaptadorResultados` devuelve un `ResultadoParseado` neutral.
Para la segunda modalidad habrá que implementar `RaceExplorerJsonParser` independiente, mapear
sus campos y decidir expresamente qué puntuaciones calculadas y estados de carrera son
pertinentes al reglamento de RaceManager. El formato `session_info.finished` puede faltar
en archivos antiguos; no se debe inferir que una sesión fue finalizada si no hay evidencia.

El repositorio Race Explorer no muestra una licencia en su raíz: se lo utilizó como **referencia
de arquitectura y formato**, no se copió su código fuente. Corresponde definir condiciones de
uso antes de incorporar componentes de terceros.

## Para completar el MVP

1. Confirmar los campos contra dos archivos actuales de una liga real, debidamente anonimizados.
2. Implementar el alta de carrera y participantes y vincular `CarId`/nombre con pilotos ya
   registrados; no asignar identidades automáticamente por coincidencia de nombres.
3. Exponer revisión y confirmación transaccional: `sesion`, `resultado_sesion`, `vuelta`.
4. Rechazar inconsistencias y asegurar que la publicación requiera una importación `CONFIRMADA`.
5. Mostrar el proceso desde React y ejecutar pruebas integrales con MySQL.
