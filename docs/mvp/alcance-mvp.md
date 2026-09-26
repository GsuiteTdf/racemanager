# RaceManager — Alcance del MVP (2da entrega)

> **Estado:** propuesta para aprobación del equipo y del tutor.
> Reemplaza la lista de funcionalidades del MVP original, que incluía prácticamente todos
> los módulos a la vez. Los criterios verificables están en
> [`criterios-aceptacion.md`](criterios-aceptacion.md).

## 1. Objetivo del MVP

Demostrar **una carrera completa, de principio a fin, con datos reales**: el organizador carga
el archivo oficial de Assetto Corsa, lo revisa, publica los resultados y un piloto los consulta.
Todo con persistencia en MySQL, una interfaz funcional y **sin intervenciones manuales en la base de datos**.

El valor de RaceManager no depende de implementar todos los módulos juntos, sino de probar que
convierte un archivo de resultados en información oficial consultable.

## 2. Flujo obligatorio

```text
 1. Manager inicia sesión ─────────► JWT + rol MANAGER + pertenencia a su liga
 2. Registra la carrera ───────────► liga propia, circuito, fecha, pilotos participantes
 3. Carga el archivo real ─────────► validación de tamaño, formato, campos y duplicados
 4. Revisa los resultados ─────────► posiciones, vueltas y tiempos antes de confirmar
 5. Publica la carrera ────────────► queda registrado quién y cuándo publicó
 6. Piloto consulta ───────────────► solo carreras PUBLICADAS: posición, tiempos, historial básico
```

| Paso | Actor | Resultado | Estados involucrados |
|---|---|---|---|
| 1 | Manager | Sesión iniciada, identifica su liga | — |
| 2 | Manager | Carrera creada con participantes | Carrera `PROGRAMADA` |
| 3 | Manager | Archivo aceptado o rechazado con motivo | Importación `PENDIENTE` → `PROCESADA` / `ERROR` |
| 4 | Manager | Datos confirmados o descartados | Importación `CONFIRMADA` / `RECHAZADA`; carrera `CARGADA` |
| 5 | Manager | Resultados oficiales | Carrera `PUBLICADA` |
| 6 | Piloto / público | Consulta de resultados | Solo `PUBLICADA` |

## 3. Qué entra y qué queda afuera

| Incluido en el MVP | Mínimo aceptable | Fuera del MVP (ampliaciones) |
|---|---|---|
| Autenticación JWT y roles | Login; roles ADMIN, MANAGER, PILOTO | Registro público abierto, recuperación de contraseña |
| Ligas | Alta y consulta de la liga propia | Categorías múltiples, temporadas comparadas |
| Pilotos | Alta de pilotos de la liga | Gestión autónoma de cuentas de equipo |
| Equipos | Precarga mínima (nombre) | Administración completa por el rol EQUIPO |
| Circuitos | Catálogo precargado | ABM completo con metadatos |
| Carreras | Alta, participantes, estados | Calendario completo, cancelaciones con notificación |
| Importación | Un formato validado con muestras reales | Otros simuladores, telemetría, sectores avanzados |
| Publicación | Revisión y publicación por el Manager | Correcciones post-publicación con historial de versiones |
| Consulta | Resultados de una carrera y historial básico del piloto | Dashboard, gráficos, comparaciones de sesiones |
| Calidad | Pruebas automáticas de seguridad e importación | Pruebas de carga, auditoría completa |
| Despliegue | Una demo accesible en la nube | Alta disponibilidad, monitoreo |

**Vehículos, estadísticas avanzadas, comparación de sesiones, dashboard, notificaciones,
exportaciones y otros simuladores** se mantienen en la propuesta como ampliaciones posteriores.

## 4. Dependencia bloqueante: archivos reales

Antes de escribir el importador hay que conseguir **al menos dos archivos reales** de una liga
(uno válido y otro con variaciones o errores), anonimizarlos y documentar su estructura en
[`../importacion/relevamiento-archivos.md`](../importacion/relevamiento-archivos.md).
No se afirma compatibilidad con un formato de Assetto Corsa sin haberlo verificado.

Hasta tener las muestras no se amplían las tablas `sesion`, `resultado_sesion` ni `vuelta`.

## 5. Validación de la problemática

Las dificultades descritas en la propuesta son **hipótesis** hasta relevarlas. Entrevistar al menos
a un organizador real y registrar:

| Dato a relevar | Pregunta guía |
|---|---|
| Proceso actual | ¿Qué hacés desde que termina una carrera hasta que se publican los resultados? |
| Herramientas | ¿Qué archivos, planillas o canales usás (Discord, Sheets, servidor)? |
| Tiempo | ¿Cuánto tiempo te lleva procesar y publicar una fecha? |
| Errores | ¿Qué errores se repiten (pilotos mal asignados, tiempos, duplicados)? |
| Volumen | ¿Cuántos pilotos, carreras por temporada y sesiones por carrera? |
| Aceptación | ¿Usarías el flujo carga → revisión → publicación? ¿Qué te falta? |

El resultado se documenta como anexo y se usa para ajustar criterios y prioridades.

## 6. Plan incremental

Cada iteración termina con algo demostrable. Duración y responsables: **a definir en la reunión de equipo**.

| Iteración | Entregable comprobable | Criterios | Depende de | Estado |
|---|---|---|---|---|
| 0 | Muestras reales, entrevista, MVP aprobado | — | Contacto con organizador | Pendiente |
| 1 | Seguridad: login JWT, roles, aislamiento por liga, pruebas negativas | CA-01, CA-02 | Modelo usuario/rol | **Implementada** y verificada en CI v3: 32 pruebas backend con H2; falta prueba E2E con frontend y MySQL |
| 2 | Alta de liga, pilotos y carrera con persistencia | CA-03 | Iteración 1 | Pendiente |
| 3 | Importador validado, transaccional, con revisión | CA-04, CA-05 | Iteraciones 0 y 2 | Parcial: parser nativo, API protegida y previsualización persistente; faltan mapeo de pilotos, revisión y confirmación |
| 4 | Publicación y consulta; frontend integrado | CA-06, CA-07 | Iteración 3 | Pendiente |
| 5 | Pruebas integrales, documentación de API y demo desplegada | CA-08 | Iteración 4 | Pendiente |

## 7. Riesgos y mitigaciones

| Riesgo | Impacto | Mitigación | Tarea concreta |
|---|---|---|---|
| Archivos de Assetto Corsa no disponibles o heterogéneos | Bloquea el importador | Conseguir muestras al inicio | Iteración 0: pedir 2+ archivos a una liga |
| MVP demasiado amplio | Entrega incompleta | Priorizar el flujo de punta a punta | Aprobar la tabla de la sección 3 |
| Acceso cruzado entre ligas | Exposición o modificación indebida | Rol + pertenencia en cada endpoint | `@ligaAccess` + prueba con dos Managers por endpoint |
| Importaciones duplicadas o parciales | Resultados incorrectos | Hash, transacción, estados y revisión | Restricciones ya en `schema.sql`; pruebas negativas en iteración 3 |
| Desajustes entre SQL y entidades JPA | Errores en ejecución | Mapear de a poco y validar | Pasar a `ddl-auto=validate` con prueba contra MySQL |
| Versiones distintas en cada PC | "En mi máquina anda" | Versiones acordadas + CI | JDK 21, Node 20+, MySQL 8.0.16+; GitHub Actions |
| Trabajo en paralelo que se pisa | Conflictos y pérdida de cambios | Branches cortas y Pull Requests | Ver `CONTRIBUTING.md` |

## 8. Decisiones abiertas para el equipo

1. ¿Un piloto puede no tener equipo o cambiar de equipo entre temporadas? (hoy `equipo_id` es obligatorio).
2. ¿La consulta de resultados publicados es pública (sin login) o solo para pilotos de la liga?
3. ¿Quién crea las cuentas de Manager: un ADMIN o un registro con aprobación?
4. ¿Qué hacer si el archivo trae un piloto que no está registrado: rechazar o proponer el alta?
5. Plataforma de despliegue de la demo (backend, base y frontend).
