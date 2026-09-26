# RaceManager — Diagnóstico, correcciones y trazabilidad (versión consolidada)

| | |
|---|---|
| **Fecha del diagnóstico inicial** | 26/09/2026 (1ra entrega, "MVP y seguridad") |
| **Fecha de consolidación** | 26/09/2026 (integración de 1ra y 2da entrega) |
| **Branch** | `feature/mejoras-mvp-2da-entrega` (fork `GsuiteTdf/racemanager`) |
| **Base** | `main` del repositorio del equipo, commit `127a38d` (sin commits nuevos a la fecha) |
| **Versión original** | [`historico/diagnostico-1ra-entrega-original.md`](historico/diagnostico-1ra-entrega-original.md) (sin cambios) |

> Documento de trazabilidad para el equipo y el tutor. No es una certificación de seguridad ni
> afirma que haya ocurrido una intrusión: los riesgos descritos son **de diseño y configuración**.
> Un hallazgo solo figura como *corregido y verificado* cuando hay una prueba ejecutada o una
> inspección reproducible que lo respalda.

## 0. Fuentes de evidencia y método

| Fuente | Qué representa |
|---|---|
| `racemanager.zip` | Línea de base: copia local de un integrante (último commit `5f6214c`, con backend, frontend y BD sin commitear). |
| `RaceManager_TFI_MVP_Seguridad_AUDITADO` (carpeta y ZIP, contenido idéntico: huella SHA-256 `2a1d338a…`) | 1ra entrega: cierre transitorio de seguridad, MVP delimitado y diagnóstico inicial. |
| `Mejoras MVP - 2da entrega` (branch con 7 commits) | 2da entrega: implementación de JWT, roles, pertenencia a liga, restricciones SQL, documentación del MVP y CI. |
| Esta consolidación (commits posteriores a `46ed017`) | Auditoría de la 2da entrega, correcciones, pruebas adicionales e integración documental. |

**Método:** revisión estática de código, configuración, SQL y documentación; comparación archivo por archivo
entre las entregas; ejecución de pruebas donde el entorno lo permitió (ver §C.2); escaneo de secretos
con `detect-secrets` sobre todos los archivos versionados.

**Estados usados en este documento**

| Estado | Significado |
|---|---|
| ✅ **Corregido y verificado** | La corrección existe y una prueba ejecutada o inspección reproducible la confirma. |
| 🟦 **Implementado, pendiente de pruebas** | El código existe y tiene pruebas escritas, pero todavía no se ejecutaron. |
| 🟨 **Parcialmente corregido** | Solo una parte del problema está resuelta. |
| ⬜ **Pendiente** | Sin corrección todavía (o riesgo aceptado temporalmente). |

---

## Parte A — Estado original del proyecto (línea de base)

> Se conserva el diagnóstico inicial. Describe el proyecto **tal como se encontró**, antes de cualquier cambio.

La copia de partida contenía una propuesta extensa, módulos documentados, un esquema SQL inicial, la
estructura de una API Spring Boot y una aplicación React/Vite con pantallas preliminares. Sin embargo:

1. **Alcance excesivo del MVP.** El MVP incluía prácticamente toda la plataforma (usuarios con cuatro roles,
   ligas, equipos, pilotos, vehículos, circuitos, carreras, importación, estadísticas, dashboard y despliegue),
   sin distinguir lo imprescindible de las ampliaciones ni fijar criterios de aceptación verificables.
2. **Módulos incompletos.** Las clases del backend (`AuthModule`, `LigaModule`, `UsuarioModule`, etc.) eran
   marcadores vacíos: no había entidades, repositorios, servicios ni controladores de negocio. El frontend
   tenía landing y un login que no se conectaba a la API.
3. **Seguridad permisiva.** `SecurityConfig.java` terminaba con `.anyRequest().permitAll()` y habilitaba
   `httpBasic` sin usuarios reales: cualquier endpoint nuevo quedaba público por defecto.
4. **Credenciales de desarrollo inseguras.** `application.properties` usaba `root` con contraseña vacía y la URL
   incluía `createDatabaseIfNotExist=true`, `useSSL=false` y `allowPublicKeyRetrieval=true`. No se encontró
   ninguna contraseña real expuesta.
5. **Sin autenticación efectiva.** No existía login, JWT, hash de contraseñas ni reglas por rol.
6. **Sin control de pertenencia a ligas.** Nada impedía que un organizador accediera a datos de otra liga.
7. **Riesgos en importaciones.** Sin validación de archivos, sin control de duplicados, sin transacciones ni
   muestras reales de Assetto Corsa; la tabla `importacion_carrera` no impedía cargas repetidas.
8. **Publicación sin controles.** El estado `PUBLICADA` podía asignarse sin registrar quién ni cuándo.
9. **Sin pruebas funcionales.** Solo existía `contextLoads()`, que además requería un MySQL local.
10. **Riesgos de integración.** La copia local tenía archivos sin commitear, `node_modules` (~94 MB), `dist`,
    cachés de Gradle y su propio `.git`, y estaba **detrás** del `main` remoto (commit `127a38d`).

Evidencia del estado original (`SecurityConfig.java` y `application.properties` de `racemanager.zip`):

```java
.requestMatchers("/api/health", "/actuator/health").permitAll()
.anyRequest().permitAll()
.httpBasic(Customizer.withDefaults());
```

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/racemanager?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=
```

---

## Parte B — Correcciones realizadas

### B.1 Primera entrega (cierre transitorio y delimitación)

| Cambio | Archivo | Decisión |
|---|---|---|
| `permitAll()` → `denyAll()`; solo `GET /api/health` público; se quita `httpBasic`; `@EnableMethodSecurity` | `backend/.../config/SecurityConfig.java` | Cerrar la superficie hasta tener autenticación real, en lugar de simular una. |
| Credenciales por `DB_URL`, `DB_USER`, `DB_PASSWORD`; se quitan `createDatabaseIfNotExist`, `useSSL=false`, `allowPublicKeyRetrieval` | `backend/src/main/resources/application.properties`, `backend/.env.example` | Sacar credenciales del código. |
| Límite de carga de 2 MB | `application.properties` | Primera barrera para el futuro importador. |
| MVP delimitado, criterios CA-01..CA-08, riesgos, plan por iteraciones, validación de la problemática | `docs/propuesta/propuesta-proyecto.md`, `docs/seguridad-y-mvp.md` | Concentrar el esfuerzo en un flujo de punta a punta. |
| Guía para incorporar el ZIP sin `.git` ni dependencias | `docs/INTEGRACION-ZIP.md` | Evitar sobrescribir el repositorio. |
| Diagnóstico y trazabilidad | `docs/auditoria/diagnostico-y-trazabilidad-2026-09-26.md` | Registro del estado inicial. |

Limitaciones que la propia 1ra entrega reconocía: JWT, roles, pertenencia, importador y pruebas quedaban pendientes;
su versión de la propuesta además **eliminaba** las secciones de viabilidad y riesgos que ya estaban en GitHub.

### B.2 Segunda entrega (implementación) — commits `5792797`…`46ed017`

| Commit | Cambio | Archivos principales |
|---|---|---|
| `5792797` | Se incorpora al repositorio el trabajo que estaba solo en local, **sobre el `main` remoto actualizado** (`127a38d`), sin `node_modules`, `dist`, `build`, `.gradle` ni `.git` anidado. | `backend/`, `frontend/`, `database/`, `docs/`, `.gitignore` |
| `ccd8020` | Java 24 (sin soporte) → **Java 21 LTS**. | `backend/build.gradle` |
| `cd85547` | **Autenticación y autorización reales:** login con BCrypt, JWT HS256 (firma, vencimiento, emisor), secreto obligatorio ≥ 32 bytes, `@PreAuthorize`, `LigaAccessGuard` (`@ligaAccess`), errores 401/403 en JSON, CORS configurable, `denyAll` fuera de `/api/**`. Pruebas unitarias y de integración (H2). | `auth/*`, `usuario/*`, `liga/*`, `config/SecurityConfig.java`, `common/ApiExceptionHandler.java`, `application.properties`, `src/test/**` |
| `22d022d` | Login del frontend conectado a la API; sesión con vencimiento; interceptor Axios con JWT y descarte ante 401. | `frontend/src/modules/auth/LoginPage.tsx`, `frontend/src/services/{api,sesion}.ts` |
| `c696df2` | **Integridad en MySQL:** hash SHA-256 por archivo, `UNIQUE (carrera_id, hash_sha256)`, una sola importación `CONFIRMADA` por carrera (columna calculada + `UNIQUE`), `CHECK` que impide publicar sin `publicada_en`/`publicada_por`; migración 002 y datos de desarrollo. | `database/schema.sql`, `database/migraciones/002_*.sql`, `database/datos-desarrollo.sql`, `docs/db/modelo-datos.md` |
| `f7f5fe9` | Alcance del MVP, criterios CA-01..CA-08 con Dado/Cuando/Entonces, definición de terminado, guía de seguridad, relevamiento de archivos; propuesta con viabilidad y riesgos **conservados** en tablas. | `docs/mvp/*`, `docs/seguridad/seguridad.md`, `docs/importacion/*`, `docs/propuesta/*`, `README.md` |
| `46ed017` | CI (backend, esquema MySQL, frontend), plantilla de PR, `CONTRIBUTING.md`. | `.github/*`, `CONTRIBUTING.md` |

Qué se tomó de la 1ra entrega y se **mejoró** en la 2da: el `denyAll` transitorio se reemplazó por autenticación
real (se conserva la denegación por defecto fuera de `/api/**`); las variables de entorno se mantuvieron y se sumó
`JWT_SECRET`; los criterios pasaron a formato verificable con estado; el control de duplicados pasó de documentado
a **garantizado por la base**.

### B.3 Consolidación (esta revisión) — commits posteriores a `46ed017`

Auditoría completa de la 2da entrega. Hallazgos nuevos (prefijo `AUD`) y correcciones:

| ID | Hallazgo | Corrección | Commit |
|---|---|---|---|
| AUD-01 | El JWT no declaraba ni exigía **audiencia**. | Claim `aud=racemanager-web` y `requireAudience`. | `4ef6bfb` |
| AUD-02 | Contraseñas de más de **72 bytes** (límite de BCrypt) podían producir un error 500 o compararse truncadas. | Rechazo con 401 y mismo tiempo de respuesta. | `4ef6bfb` |
| AUD-03 | Orígenes CORS con espacios tras la coma no coincidían. | `trim()` de cada origen. | `4ef6bfb` |
| AUD-04 | Clave JWT fija en las propiedades de prueba; contraseñas fijas en pruebas y en el workflow de CI. | Clave y contraseñas aleatorias por ejecución; MySQL de CI sin contraseña versionada. | `4ef6bfb`, `e340bfe` |
| AUD-05 | Nada impedía que un endpoint nuevo quedara accesible para **cualquier** usuario autenticado (p. ej. un PILOTO) por olvidar `@PreAuthorize`. | `EndpointsProtegidosTest`: falla si un endpoint no declara `@PreAuthorize` (excepto health, login y `/me`). | `4ef6bfb` |
| AUD-06 | Faltaban pruebas de rol EQUIPO, ADMIN sobre liga ajena, token falsificado con rol ADMIN, encabezado no Bearer y rutas no declaradas. | Pruebas agregadas a `SeguridadIntegracionTest`. | `4ef6bfb` |
| AUD-07 | El frontend no tenía pruebas; la primera versión de Vitest elegida tenía un aviso de seguridad. | Vitest 5 + jsdom, 7 pruebas; `npm audit` sin vulnerabilidades; CI con `npm test` y `npm audit`. | `308885b` |
| AUD-08 | Las restricciones SQL solo se habían probado a mano. | `database/pruebas/verificar-restricciones.sql` + CI sobre base nueva y migrada. | `02cf7c7` |
| AUD-09 | Sin JDK 21 local, Gradle fallaba en vez de obtenerlo. | Plugin `foojay-resolver-convention`. | `679d8b0` |
| AUD-10 | El README de la 2da entrega había quitado contenido que ya estaba en GitHub (herramientas y tabla de roadmap). | Contenido restituido y actualizado con el estado real. | commit `docs: integrar la auditoría…` |
| AUD-11 | El remoto `origin` del repositorio local apuntaba al repositorio del equipo y no al fork. | `origin` → `GsuiteTdf/racemanager`; `upstream` → `ClauRodriguez/racemanager` **con push deshabilitado**. | configuración local (no versionada) |

Revisado sin cambios necesarios: expiración del token (60 min por defecto, configurable, > 0 obligatorio);
rechazo de tokens sin firma, alterados, vencidos o de otro emisor; mensaje de login único para email inexistente,
contraseña incorrecta y usuario inactivo, con comparación BCrypt siempre ejecutada; filtro JWT fuera del contexto
de servlet (no se registra dos veces); ausencia de manejador genérico de `Exception` (no oculta los 403);
`server.error.include-message=never` y `include-stacktrace=never`; compatibilidad **Java 21 + Spring Boot 4.0.0 +
Gradle 9.7.1 + jjwt 0.12.6** (Spring Boot 4 admite Java 17 a 25; revisión documental, no compilada, ver §C.2).

---

## Parte C — Estado consolidado

### C.1 Matriz de trazabilidad

| ID | Problema original | Evidencia | Riesgo | Corrección aplicada | Archivo(s) | Prueba de verificación | Estado |
|---|---|---|---|---|---|---|---|
| SEG-01 | `anyRequest().permitAll()`: todo endpoint nuevo quedaba público. | `SecurityConfig.java` de la línea de base. | Alto | 1ra: `denyAll`. 2da: públicos solo health y login; `/api/**` autenticado; resto `denyAll`. | `config/SecurityConfig.java` | `SeguridadIntegracionTest`: `sinTokenResponde401`, `rutaNoDeclaradaNoEsAccesibleSinAutenticacion` | 🟦 Implementado, pendiente de pruebas |
| SEG-02 | `httpBasic` habilitado sin usuarios; confusión entre mecanismo y protección. | Idem. | Medio | Se desactivan httpBasic, formLogin y logout de sesión; `UserDetailsService` vacío evita el usuario en memoria. | `config/SecurityConfig.java` | `encabezadoAuthorizationSinBearerResponde401` | 🟦 Implementado, pendiente de pruebas |
| SEG-03a | Usuario `root` y contraseña vacía en el código. | `application.properties` de la línea de base. | Medio | Variables `DB_URL`, `DB_USER`, `DB_PASSWORD`; `.env.example` sin valores reales. | `application.properties`, `backend/.env.example` | Escaneo `detect-secrets` de todos los archivos versionados: solo falsos positivos por palabra clave. | ✅ Corregido y verificado |
| SEG-03b | Sin usuario MySQL de mínimo privilegio ni política TLS. | Idem. | Medio | Usuario de aplicación documentado con `GRANT` mínimos. TLS a definir con el despliegue. | `database/README.md` | — | 🟨 Parcialmente corregido |
| SEG-04 | Sin autenticación efectiva (login, hash, tokens). | Clases marcadoras en `auth/`, `usuario/`. | Alto | Login BCrypt; JWT HS256 con firma, vencimiento, emisor y audiencia; secreto obligatorio ≥ 32 bytes; mensaje de error único; rechazo de inactivos y de contraseñas > 72 bytes. | `auth/*`, `usuario/*` | `JwtServiceTest` (8 casos), `SeguridadIntegracionTest` (login correcto, credenciales inválidas, 400, > 72 bytes) | 🟦 Implementado, pendiente de pruebas |
| SEG-05 | Sin permisos por rol. | Sin `@PreAuthorize`. | Alto | `@PreAuthorize` por endpoint; `EndpointsProtegidosTest` impide endpoints sin regla. | `liga/LigaController.java`, `src/test/.../EndpointsProtegidosTest.java` | `pilotoNoAccedeALaGestionDeLigas`, `equipoNoAccedeALaGestionDeLigas`, `adminVeTodasLasLigas` | 🟦 Implementado, pendiente de pruebas |
| SEG-06 | Sin control de pertenencia: un organizador podía acceder a otra liga cambiando el id. | Sin servicios ni reglas. | Alto | `LigaAccessGuard`: ADMIN todas, MANAGER solo `manager_id` propio, liga inexistente = 403. Hoy solo existen endpoints de **consulta** de ligas; no hay endpoints de modificación. | `liga/LigaAccessGuard.java` | `managerAccedeASuLigaPeroNoALaAjena`, `ligaInexistenteSeTrataComoAjena`, `LigaAccessGuardTest` (5 casos) | 🟦 Implementado, pendiente de pruebas (extender a carreras, pilotos e importaciones) |
| SEG-07 | Archivos importados repetidos o múltiples resultados confirmados. | `importacion_carrera` sin restricciones. | Alto | `UNIQUE (carrera_id, hash_sha256)`; una sola `CONFIRMADA` por carrera. | `database/schema.sql`, `migraciones/002_*.sql` | `verificar-restricciones.sql` ejecutado en MySQL 8.0.46 (base nueva y migrada): OK. Evidencia: `evidencias/2026-09-26-mysql.txt` | ✅ Corregido y verificado (nivel base de datos) |
| SEG-08 | Validación de archivos, transacción y parser inexistentes. | Sin importador. | Alto | Límite de 2 MB; reglas documentadas (tipo, estructura, nombre generado, transacción). Parser bloqueado por falta de muestras. | `application.properties`, `docs/seguridad/seguridad.md` | — | 🟨 Parcialmente corregido |
| SEG-09 | Publicación sin auditoría ni revisión previa. | `carrera.estado` sin controles. | Medio | `CHECK`: `PUBLICADA` exige `publicada_en` y `publicada_por`. Falta la lógica de revisión/publicación y el filtro de consultas públicas. | `database/schema.sql` | `verificar-restricciones.sql` (casos 4 y 5): OK | 🟨 Parcialmente corregido (restricción verificada; servicios pendientes) |
| SEG-10 | Frontend sin autenticación ni manejo seguro del token. | `LoginPage.tsx` sin conexión. | Medio | Login real, token en `sessionStorage` con vencimiento, interceptor con `Bearer` y descarte ante 401; los roles del cliente solo ajustan la interfaz. | `frontend/src/services/*`, `LoginPage.tsx` | Vitest: 7 pruebas OK; build `tsc` estricto OK; `npm audit` 0. Evidencia: `evidencias/2026-09-26-frontend.txt` | ✅ Corregido y verificado (unitario); prueba E2E con backend pendiente |
| TST-01 | Sin pruebas funcionales; `contextLoads` dependía de MySQL. | `RacemanagerApiApplicationTests.java`. | Alto | Perfil `test` con H2; pruebas de seguridad; pruebas frontend; prueba SQL; CI con los tres componentes. | `src/test/**`, `frontend/src/**/*.test.ts`, `database/pruebas/*`, `.github/workflows/ci.yml` | Frontend y SQL ejecutados OK. **Backend no ejecutado** (ver §C.2). | 🟨 Parcialmente corregido |
| CFG-01 | Java 24 sin soporte. | `build.gradle`. | Medio | Java 21 LTS + resolución automática del JDK. | `build.gradle`, `settings.gradle` | Revisión de compatibilidad; compilación no ejecutada. | 🟦 Implementado, pendiente de pruebas |
| CFG-02 | Claves fijas en pruebas y CI. | `application-test.properties`, `ci.yml` de la 2da entrega. | Bajo | Valores aleatorios; MySQL de CI sin contraseña versionada. | Idem | Escaneo `detect-secrets` | ✅ Corregido y verificado |
| DOC-01 | MVP con casi toda la plataforma. | Propuesta original. | Alto | Flujo de punta a punta; tabla incluido/mínimo/fuera; plan por iteraciones. | `docs/mvp/alcance-mvp.md`, propuesta | Revisión de coherencia con el código (esta auditoría) | 🟨 Parcialmente corregido (falta aprobación del equipo y tutor) |
| DOC-02 | Sin criterios de aceptación verificables. | Propuesta original. | Medio | CA-01..CA-08 con evidencia y estado; definición de terminado. | `docs/mvp/criterios-aceptacion.md` | Estados cotejados con el código real | ✅ Corregido y verificado |
| DOC-03 | Problemática y formato de archivos sin validar. | Sin relevamiento ni muestras. | Alto | Guía de entrevista y plantilla de relevamiento. | `docs/mvp/alcance-mvp.md` §5, `docs/importacion/relevamiento-archivos.md` | — | ⬜ Pendiente (entrevista y muestras) |
| DOC-04 | Plan sin responsables ni fechas. | Roadmap original. | Medio | Iteraciones 0–5 con dependencias y entregables. | `docs/mvp/alcance-mvp.md` §6 | — | 🟨 Parcialmente corregido (faltan responsables y fechas) |
| INT-01 | ZIP con `.git` propio, `node_modules`, `dist` y cachés. | `racemanager.zip`. | Medio | Incorporación selectiva sobre `main` remoto; `.gitignore` ampliado. | `.gitignore` | `git ls-files` sin `node_modules`, `dist`, `build`, `.gradle`, `.env` ni `.git` anidado | ✅ Corregido y verificado |
| INT-02 | Copia local detrás del remoto; riesgo de perder `127a38d` (viabilidad y riesgos). | `git log` del ZIP (`5f6214c`). | Medio | Branch creada desde `127a38d`; viabilidad y riesgos conservados. | `docs/propuesta/propuesta-proyecto.md` | `git merge-base --is-ancestor upstream/main HEAD` OK; ningún archivo de `main` eliminado | ✅ Corregido y verificado |
| INT-03 | `origin` apuntaba al repositorio del equipo (AUD-11). | `git remote -v`. | Alto | `origin` = fork; `upstream` = equipo con push deshabilitado. | configuración local | `git remote -v` | ✅ Corregido y verificado |
| AUD-12 | Un token sigue válido hasta vencer aunque el usuario se desactive; no hay revocación. | `JwtAuthenticationFilter`. | Medio | Mitigación: vencimiento corto (60 min). | — | — | ⬜ Pendiente |
| AUD-13 | Sin límite de intentos de login (fuerza bruta). | `AuthController`. | Medio | — | — | — | ⬜ Pendiente (antes de desplegar) |
| AUD-14 | Token en `sessionStorage`: legible si existiera una falla XSS. | `sesion.ts`. | Bajo/Medio | Riesgo aceptado: React escapa el contenido, no se usa `dangerouslySetInnerHTML`, el token vence. Falta política CSP en el despliegue. | — | — | ⬜ Pendiente (riesgo aceptado) |
| AUD-15 | `datos-desarrollo.sql` contiene el hash de una contraseña documentada. | Archivo versionado. | Bajo | Marcado "solo desarrollo"; nunca ejecutar en producción. | `database/datos-desarrollo.sql` | — | ⬜ Pendiente (riesgo aceptado para desarrollo) |
| AUD-16 | Entidades JPA no validadas contra MySQL (`ddl-auto=none`). | `application.properties`. | Medio | Pasar a `validate` con una prueba contra MySQL. | — | — | ⬜ Pendiente |

### C.2 Pruebas ejecutadas y no ejecutadas

| Componente | Comando | Resultado | Evidencia |
|---|---|---|---|
| Base de datos (MySQL 8.0.46) | `schema.sql` + `datos-desarrollo.sql` + `pruebas/verificar-restricciones.sql` | ✅ OK | `evidencias/2026-09-26-mysql.txt` |
| Migración 002 | Esquema v1 + migración + prueba de restricciones; comparación estructural con el esquema nuevo | ✅ OK, estructuras idénticas | idem |
| Control negativo SQL | Prueba de restricciones sobre el esquema v1 sin migrar | ✅ Falla como se espera | idem |
| Frontend | `npm test` (Vitest, 7 pruebas), `npm run build`, `npm audit` | ✅ OK, 0 vulnerabilidades | `evidencias/2026-09-26-frontend.txt` |
| Secretos | `detect-secrets scan` sobre `git ls-files` | ✅ Sin secretos (2 falsos positivos por palabra clave) | este documento |
| **Backend** | `./gradlew test` (JDK 21) | ⚠️ **No ejecutado**: el entorno no tenía acceso a `services.gradle.org`, `plugins.gradle.org` ni `repo.maven.apache.org` (proxy 403) | `evidencias/2026-09-26-backend-intento.txt` |

Las **32 pruebas del backend** (`JwtServiceTest`, `LigaAccessGuardTest`, `SeguridadIntegracionTest`,
`EndpointsProtegidosTest`, `RacemanagerApiApplicationTests`) están escritas pero **no se consideran superadas**
hasta que corran en GitHub Actions o en la PC de un integrante. Por eso los hallazgos que dependen de ellas
figuran como 🟦. Al obtener un resultado, actualizar esta tabla y la matriz.

### C.3 Tareas pendientes (priorizadas)

1. **Ejecutar las pruebas del backend** (CI del Pull Request o `.\gradlew.bat test` con JDK 21) y corregir lo que falle.
2. Aprobar con el equipo y el tutor el alcance del MVP y las decisiones abiertas (`docs/mvp/alcance-mvp.md` §8).
3. Conseguir 2 archivos reales anonimizados de Assetto Corsa y entrevistar a un organizador (DOC-03).
4. Iteración 2: alta de liga, pilotos y carrera con `@ligaAccess` y la prueba "Manager A contra recurso de B".
5. Iteración 3: importador transaccional con validación y hash (SEG-08) sobre las restricciones ya verificadas.
6. Iteración 4: servicios de revisión/publicación y consultas que filtren `PUBLICADA` (SEG-09).
7. Antes de desplegar: límite de intentos de login (AUD-13), CSP y HTTPS (AUD-14), usuario MySQL mínimo y TLS
   (SEG-03b), `ddl-auto=validate` (AUD-16), revocación o verificación de usuario activo (AUD-12).

**Conclusión:** respecto de la línea de base, la superficie abierta por defecto se cerró, existe autenticación y
autorización por rol y por liga con pruebas escritas, la base de datos impide duplicados y publicaciones sin
auditoría (verificado), y el frontend maneja la sesión con pruebas. El circuito completo de importación y
publicación **no está implementado** y las pruebas del backend **aún no se ejecutaron**: el proyecto no debe
presentarse como MVP terminado ni como seguro para producción.
