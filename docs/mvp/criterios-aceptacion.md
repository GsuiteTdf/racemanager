# RaceManager — Criterios de aceptación del MVP

Cada criterio se considera **cumplido** solo cuando existe la evidencia indicada (prueba automática
que pasa en CI o demostración reproducible). Formato: *Dado / Cuando / Entonces*.

Alcance y flujo: [`alcance-mvp.md`](alcance-mvp.md).

## Resumen

| ID | Criterio | Evidencia | Estado |
|---|---|---|---|
| CA-01 | Autenticación y protección de endpoints | `SeguridadIntegracionTest`, `JwtServiceTest`, `EndpointsProtegidosTest` | ✅ Verificado: CI v3 (`36270398824`), 32 pruebas con H2; prueba E2E pendiente |
| CA-02 | Aislamiento entre ligas | `SeguridadIntegracionTest`, `LigaAccessGuardTest` | ✅ Verificado en CI para endpoints de consulta existentes; extender a cualquier endpoint nuevo |
| CA-03 | Persistencia de carrera y participantes | Prueba de integración con MySQL | ⬜ Pendiente (iteración 2) |
| CA-04 | Importación de un archivo real | Prueba con archivo anonimizado | 🟨 Parser nativo, API y previsualización implementados con fixture sintética; prueba con archivo reciente y confirmación pendientes |
| CA-05 | Rechazo de archivos inválidos, excesivos o repetidos | Pruebas negativas | 🟨 Validación JSON, tamaño, integridad y duplicados en backend; restricciones MySQL existentes; pendientes casos reales y confirmación |
| CA-06 | Revisión y publicación controladas | Pruebas de roles y estados | 🟨 `CHECK` de publicación auditada **verificado en MySQL**; servicios y filtro de consultas pendientes |
| CA-07 | Resultados publicados fieles al archivo | Comparación contra muestra de referencia | ⬜ Pendiente |
| CA-08 | Instalación reproducible y API documentada | Instalación desde README en una PC limpia | 🟨 README y CI listos; falta OpenAPI y prueba en PC limpia |

Estados: ✅ corregido y verificado · 🟦 implementado, pendiente de pruebas · 🟨 parcial · ⬜ pendiente.
Evidencia de ejecución: [`../auditoria/evidencias/`](../auditoria/evidencias/).

## Detalle

### CA-01 — Autenticación y protección de endpoints

- **Dado** un usuario activo con credenciales válidas, **cuando** llama a `POST /api/auth/login`,
  **entonces** recibe `200` con un JWT que vence en el tiempo configurado.
- **Dado** un email inexistente, una contraseña incorrecta o un usuario inactivo, **cuando** intenta
  iniciar sesión, **entonces** recibe `401` con el **mismo** mensaje en los tres casos y sin token.
- **Dado** una solicitud sin token, con token alterado, vencido o de otro emisor, **cuando** llama a
  cualquier endpoint de `/api/**` distinto de health y login, **entonces** recibe `401`.
- **Dado** que falta `JWT_SECRET` o tiene menos de 32 caracteres, **cuando** se inicia la API,
  **entonces** no arranca y explica el motivo.

*Evidencia:* `SeguridadIntegracionTest`, `JwtServiceTest` (incluye token de otra audiencia y contraseña > 72 bytes).

### CA-02 — Aislamiento entre ligas

- **Dado** el Manager A y la liga de B, **cuando** A consulta, modifica o importa sobre la liga de B
  (o sobre una carrera, piloto o importación de esa liga), **entonces** recibe `403` y no se modifica nada.
- **Dado** un id de liga inexistente, **entonces** la respuesta es la misma que para una liga ajena (`403`).
- **Dado** un usuario PILOTO o EQUIPO, **cuando** usa un endpoint de gestión, **entonces** recibe `403`.
- **Dado** un ADMIN, **entonces** puede consultar cualquier liga.
- **Dado** un endpoint nuevo sin `@PreAuthorize`, **entonces** `EndpointsProtegidosTest` falla.

*Evidencia:* `SeguridadIntegracionTest`, `LigaAccessGuardTest`.
*Regla para el equipo:* **cada endpoint nuevo** que reciba un id de liga o de un recurso de una liga
debe usar `@ligaAccess` y sumar una prueba "Manager A contra recurso de B".

### CA-03 — Persistencia de carrera y participantes

- **Dado** un Manager autenticado, **cuando** crea una carrera en su liga con circuito, fecha y pilotos,
  **entonces** queda en estado `PROGRAMADA` en MySQL y puede consultarse luego.
- **Dado** un piloto que pertenece a otra liga, **cuando** se lo intenta agregar como participante,
  **entonces** se rechaza con `400`/`403`.

### CA-04 — Importación de un archivo real

- **Dado** un archivo real válido (anonimizado) de Assetto Corsa, **cuando** el Manager lo carga,
  **entonces** la importación pasa a `PROCESADA` y muestra pilotos, posiciones, vueltas y tiempos
  sin tocar la base manualmente.
- **Dado** un piloto del archivo que no está registrado, **entonces** se informa para resolverlo
  (según la decisión abierta 4 de `alcance-mvp.md`).

### CA-05 — Rechazo de archivos inválidos, excesivos o repetidos

- Archivo mayor a 2 MB → rechazado antes de procesarse.
- Formato o contenido inválido → importación en `ERROR` con mensaje entendible; **no** se guardan resultados parciales.
- Mismo archivo (mismo SHA-256) para la misma carrera → rechazado (`409`).
- Segunda importación confirmada para una carrera → imposible (restricción en la base, verificada por `database/pruebas/verificar-restricciones.sql`).

### CA-06 — Revisión y publicación controladas

- Solo el Manager de la liga (o ADMIN) puede confirmar, rechazar y publicar.
- No se puede publicar una carrera sin una importación `CONFIRMADA`.
- Toda carrera `PUBLICADA` registra `publicada_en` y `publicada_por` (restricción `CHECK`).
- Una carrera `PROGRAMADA` o `CARGADA` no aparece en consultas de pilotos ni públicas.

### CA-07 — Resultados publicados fieles al archivo

- Para una muestra de referencia, las posiciones, vueltas y tiempos publicados coinciden con los del
  archivo de origen (comparación automatizada contra un resultado esperado).

### CA-08 — Instalación reproducible y API documentada

- Un integrante instala el sistema en una PC limpia siguiendo solo el README.
- Los endpoints principales están documentados (OpenAPI/Swagger o colección Postman versionada).
- La demo desplegada completa el flujo del MVP.

## Definición de terminado (aplica a cada tarea / Pull Request)

Una tarea está **terminada** cuando:

1. Cumple los criterios de aceptación que le corresponden.
2. Tiene pruebas automáticas (incluidas las negativas de seguridad si expone un endpoint).
3. El build y las pruebas pasan en CI.
4. Otro integrante revisó y aprobó el Pull Request.
5. No incluye secretos, `.env`, `node_modules`, `build`, `dist` ni archivos generados.
6. La documentación afectada (README, esquema, endpoints) está actualizada.
