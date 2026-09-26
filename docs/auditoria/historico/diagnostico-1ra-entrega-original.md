> **Documento histórico — 1ra entrega (26/09/2026).** Se conserva sin cambios de contenido como registro de la auditoría inicial.
> La versión vigente y consolidada es [`../diagnostico-y-trazabilidad-2026-09-26.md`](../diagnostico-y-trazabilidad-2026-09-26.md).

# RaceManager — diagnóstico inicial, falencias, riesgos y trazabilidad de cambios

**Fecha de revisión:** 26/09/2026  
**Estado del documento:** auditoría estática de la copia local entregada por un integrante y comparación con la entrega «MVP y seguridad».  
**Alcance de la evidencia:** archivos contenidos en `racemanager.zip` (línea de base local) y `RaceManager_TFI_MVP_Seguridad_FINAL.zip` (versión modificada). No se auditó el repositorio remoto actual, no se desplegó la aplicación y no se ejecutaron pruebas integrales contra MySQL.  
**Uso:** documento de trazabilidad para el equipo y el tutor; no constituye una certificación de seguridad ni afirma que haya ocurrido una intrusión.

## 1. Resumen del diagnóstico anterior a los cambios

La copia de partida ya contenía una propuesta extensa, diagrama y módulos documentados, esquema SQL inicial, estructura de API Spring Boot y una aplicación React/Vite con navegación y pantallas preliminares. Sin embargo, la mayoría de los módulos del backend eran clases marcadoras; no existían entidades/repositorios/servicios y controladores de negocio operativos. El frontend no disponía aún de autenticación real ni del flujo de carga/publicación. No había evidencia de importación de archivos reales de Assetto Corsa ni de pruebas funcionales del recorrido integral.

La propuesta inicial presentaba como MVP numerosos módulos de plataforma (gestión amplia de usuarios, ligas, equipos, pilotos, vehículos, circuitos, análisis y dashboard). El alcance no distinguía suficientemente entre funciones imprescindibles para demostrar la propuesta de valor y ampliaciones posteriores. Tampoco fijaba criterios de aceptación individualizados y verificables, ni había evidencia documental de relevamiento con organizadores reales o muestras reales del formato de resultados.

En la configuración inicial se observaron riesgos de seguridad *por diseño/configuración*, no incidentes ni explotación demostrada: todos los endpoints quedaban permitidos por `.anyRequest().permitAll()`; se configuraba el usuario `root` y contraseña vacía en `application.properties`; y aún no existían JWT, reglas por rol, aislamiento de ligas ni un importador con validación/controles de duplicación. La cadena de seguridad inicial también habilitaba HTTP Basic; no debe confundirse esa habilitación con un mecanismo de autorización efectivo mientras `.anyRequest().permitAll()` permanezca activo.

## 2. Método, evidencia y límites

- **Método:** lectura estática de documentación, configuración, estructura de código y esquema SQL; cotejo antes/después de archivos modificados o nuevos. Se clasifican los hallazgos como «corregido en archivos», «mitigado temporalmente», «documentado/no implementado» o «requiere prueba».
- **Línea de base local:** `racemanager.zip`, especialmente `backend/src/main/java/com/racemanager/api/config/SecurityConfig.java`, `backend/src/main/resources/application.properties`, `docs/propuesta/propuesta-proyecto.md`, `backend/src/main/java/com/racemanager/api/**`, `frontend/src/**` y `database/schema.sql`.
- **Entrega comparada:** `RaceManager_TFI_MVP_Seguridad_FINAL.zip`. Este documento añade una capa de trazabilidad a esa entrega; los cambios aquí relatados son modificaciones **en archivos**, no cambios ya aprobados, probados o fusionados en `main` remoto.
- **No realizado:** escaneo dinámico de seguridad, pruebas de penetración, ejecución de Gradle/JUnit con base de datos, compilación verificada en los equipos del grupo, validación real del parser, auditoría de dependencias, merge o despliegue.

## 3. Matriz de hallazgos y cambios aplicados

| ID | Situación previa y evidencia | Riesgo o efecto | Acción en la entrega | Estado verificable / restante |
|---|---|---|---|---|
| SEG-01 | `SecurityConfig.java`: `.anyRequest().permitAll()` después de dos excepciones públicas. | Toda nueva ruta de negocio podría quedar públicamente accesible al implementarse. | Reemplazo por `.requestMatchers(HttpMethod.GET, "/api/health").permitAll()` y `.anyRequest().denyAll()`. | **Mitigación temporal aplicada en código**: acceso general denegado. Debe confirmarse con pruebas HTTP; JWT/roles aún no implementados. |
| SEG-02 | `SecurityConfig.java`: `.httpBasic(Customizer.withDefaults())`, sin cadena JWT ni identidad basada en token. | Configuración de autenticación incompleta para los requisitos propuestos; riesgo de confundir un mecanismo habilitado con protección efectiva. | Se elimina la habilitación explícita de HTTP Basic y se documenta la futura cadena JWT. | **Eliminado el Basic explícito; JWT pendiente.** Los endpoints de negocio siguen cerrados. |
| SEG-03 | `application.properties`: `spring.datasource.username=root` y `spring.datasource.password=`; URL local con `createDatabaseIfNotExist=true`, `useSSL=false`, `allowPublicKeyRetrieval=true`. | Incentiva credenciales privilegiadas o vacías y opciones de conexión no apropiadas como configuración genérica. **No se halló una contraseña real expuesta** en esta copia. | URL, usuario y clave mediante `DB_URL`, `DB_USER` y `DB_PASSWORD`; ejemplo en `backend/.env.example`; se retiran los parámetros antes indicados de la URL predeterminada. | **Configuración mejorada**, pero crear usuario MySQL de privilegios mínimos, definir clave real fuera de Git y revisar TLS para el despliegue siguen pendientes. El valor de `DB_PASSWORD` puede quedar vacío si no se configura; no se garantiza arranque seguro sin validación operativa. |
| SEG-04 | No existen endpoints/servicios con autorización por rol ni verificación de pertenencia a liga. | Futuro acceso o modificación cruzada entre organizadores si se autorizara solo por rol o IDs suministrados por cliente. | Se documentan verificaciones obligatorias de rol **y** propiedad de liga (`manager_id`); se prohíbe habilitar los endpoints antes de introducirlas. | **Documentado, no implementado**. Pruebas con dos ligas y dos usuarios obligatorias. |
| SEG-05 | No hay parser/importador funcional, ni casos negativos comprobados, ni contrato avalado por archivos reales. | Archivos erróneos o repetidos y resultados inconsistentes al desarrollar la carga. | Límite `multipart` de 2 MB; contrato de muestras; propuesta de transacciones, hash/reintentos y revisión antes de publicar. | **Límite configurado; validación real, parser, antivirus si corresponde, deduplicación y atomicidad pendientes.** El límite de 2 MB es inicial y se ajustará con muestras reales. |
| SEG-06 | Propuesta de resultados y publicación, sin servicios que restrinjan las consultas por estado. | Posible difusión prematura si las consultas futuras no filtran carreras pendientes. | Se incorpora la regla «solo PUBLICADA es visible» y revisión/confirmación del Manager al contrato del MVP. | **Diseño documentado, no implementado**. |
| DOC-01 | El MVP inicial abarcaba casi toda la plataforma y funciones analíticas. | Dispersión de esfuerzo y entrega incompleta. | Se incorpora el flujo mínimo: login → liga/participantes/carrera → archivo real → validación → revisión → publicación → consulta. Se documenta el fuera de alcance. | **Delimitación propuesta; pendiente aprobación del grupo/tutor.** |
| DOC-02 | No había matriz explícita de aceptación verificable para el flujo. | No se podía demostrar de forma uniforme cuándo una funcionalidad estaba terminada. | Criterios CA-01 a CA-08 con evidencias exigibles y plan incremental. | **Documentado; pruebas no ejecutadas.** |
| DOC-03 | No constaba validación empírica de la problemática ni archivos genuinos. | Riesgo de desarrollar supuestos incorrectos sobre el organizador o el formato del simulador. | Relevamiento a organizadores y obtención de al menos dos muestras anonimizadas como condición previa al parser. | **Pendiente relevamiento y recepción de muestras.** |
| DOC-04 | Falta de responsables, duración y dependencias aceptadas por el equipo. | Plan difícil de coordinar. | Iteraciones 0–5 con dependencias y entregables propuestos. | **No se asignaron responsables ni fechas**; deben acordarse con equipo/tutor. |
| INT-01 | El ZIP inicial incluía `.git` propio, dependencias de frontend y archivos generados. | Sobrescritura accidental de repositorio, crecimiento innecesario y archivos ajenos al código. | Entrega limpia sin `.git`, `node_modules`, `build`, `dist` ni cachés; `docs/INTEGRACION-ZIP.md`. | **Paquete saneado.** Incorporar selectivamente mediante una branch, revisión y PR; no se efectuó merge remoto. |

## 4. Comparación puntual de configuración de seguridad

### 4.1 Antes — configuración inicial

Archivo `backend/src/main/java/com/racemanager/api/config/SecurityConfig.java`:

```java
.requestMatchers("/api/health", "/actuator/health").permitAll()
.anyRequest().permitAll()
.httpBasic(Customizer.withDefaults());
```

Esta combinación dejaba abiertas, por configuración, las rutas no contempladas expresamente. En el ZIP inicial los endpoints de negocio todavía no estaban implementados, por lo que **no se constató la exposición efectiva de datos reales**; el riesgo era especialmente relevante al continuar el desarrollo.

### 4.2 Después — cierre transitorio de superficie

```java
.requestMatchers(HttpMethod.GET, "/api/health").permitAll()
.anyRequest().denyAll()
```

La entrega deshabilita la apertura general y elimina el Basic explícito; activa `@EnableMethodSecurity` para facilitar las políticas futuras. Esto **no equivale** a disponer de login seguro, JWT, permisos por rol o pertenencia: ninguna ruta de negocio debería abrirse sin esos controles y sus pruebas. El CSRF continúa deshabilitado por el diseño previsto de API REST sin sesión (`STATELESS`); revisar esta decisión si se decide autenticar con cookies/sesiones en navegador.

### 4.3 Credenciales y conectividad

**Antes:** conexión codificada a localhost con usuario `root`, contraseña vacía y flags `useSSL=false`, `allowPublicKeyRetrieval=true`, `createDatabaseIfNotExist=true`.  
**Después:** URL configurable `DB_URL`, usuario `DB_USER`, contraseña `DB_PASSWORD`, y `backend/.env.example` de referencia (sin credenciales reales). `spring.jpa.open-in-view=false` y `ddl-auto=none` ya estaban configurados antes; **no se atribuyen como correcciones nuevas**.

**Limitaciones:** falta aprovisionar un usuario dedicado con privilegios mínimos, establecer secretos y política TLS para el entorno objetivo, verificar que los valores estén realmente presentes al arrancar y documentar su administración. Una variable de entorno por sí sola no garantiza el resguardo operacional de secretos.

## 5. Delimitación del MVP acordable

**Propuesta de flujo prioritario:** el Manager se autentica, crea o gestiona su liga, registra un mínimo de pilotos y una carrera, sube un archivo real de resultados, revisa un borrador validado y confirma/publica; un participante o visitante consulta **solo** los resultados publicados.

**Fuera del primer incremento:** telemetría en tiempo real, conexión directa con servidores, múltiples simuladores, app móvil, notificaciones, estadísticas/comparaciones avanzadas, sanciones automáticas y administración histórica compleja. La propuesta completa puede conservarse como visión futura; este recorte requiere acuerdo formal del grupo y, cuando corresponda, del tutor.

**Dependencias críticas:** muestra genuina de Assetto Corsa antes del diseño definitivo del DTO/parser y entrevista breve con al menos un organizador antes de dar por probada la problemática.

## 6. Pruebas y evidencias necesarias antes de declarar cerrados los hallazgos

| ID de prueba | Procedimiento esperado | Resultado que debe acreditarse | Estado al emitir el diagnóstico |
|---|---|---|---|
| T-SEG-01 | Llamar anónimamente a una futura ruta de negocio. | No permite acceso; verificar código HTTP concreto según configuración real. | No ejecutada; no hay rutas de negocio implementadas. |
| T-SEG-02 | `GET /api/health` con API levantada. | Ruta pública responde sin filtrar detalles sensibles. | No ejecutada. |
| T-SEG-03 | JWT ausente, alterado y expirado; roles distintos. | Rechazo; no concede privilegios desde datos del frontend. | Bloqueada: JWT pendiente. |
| T-SEG-04 | Usuario A intenta consultar/modificar liga de usuario B. | Denegación incluso usando IDs válidos de B. | Bloqueada: entidades/servicios pendientes. |
| T-SEG-05 | Carrera en borrador consultada públicamente. | No visible ni directa ni indirectamente. | Bloqueada: publicación pendiente. |
| T-IMP-01 | Archivo real válido de Assetto Corsa. | Carga, persistencia y vista previa consistentes con los datos fuente. | Bloqueada: muestras/parser pendientes. |
| T-IMP-02 | Archivo malformado, excesivo y duplicado. | Rechazo controlado sin escritura/publicación parcial. | Bloqueada: importador pendiente. |
| T-INT-01 | Instalar en entorno limpio y ejecutar frontend + backend + MySQL. | Pasos reproducibles, compilación y demostración CA-01 a CA-08. | No ejecutada. |

**Criterio para cerrar seguridad:** implementar autenticación real y autorizaciones, pasar pruebas negativas automatizadas, verificar dependencias/versiones y revisar configuración del entorno de despliegue. Hasta entonces, el cierre por defecto es una **mitigación temporal**, no una remediación completa.

## 7. Integración y control de cambios

La copia del compañero contenía avances locales. El ZIP de esta entrega **no contiene `.git` ni acredita que sus archivos estén fusionados con el GitHub de origen**. El equipo debe comparar contra el clon actualizado del repositorio original, incorporar por etapas (preferiblemente documentación/configuración separadas de código funcional), ejecutar `git diff`, pruebas y revisión, y abrir PR para aprobación. No copiar nunca la carpeta `.git` del ZIP inicial ni publicar claves `.env`.

**Archivos modificados o agregados por la entrega previa:** `README.md`, `docs/propuesta/propuesta-proyecto.md`, `docs/seguridad-y-mvp.md` (nuevo), `docs/INTEGRACION-ZIP.md` (nuevo), `backend/src/main/java/com/racemanager/api/config/SecurityConfig.java`, `backend/src/main/resources/application.properties`, `backend/.env.example` (nuevo). Este documento de auditoría y los enlaces incorporados al README constituyen la ampliación documental posterior.

## 8. Registro de decisiones pendientes

1. **Equipo y tutor:** aprobar o corregir alcance MVP, exclusiones, métricas y tiempos; asignar responsables.
2. **Equipo/organizador:** obtener muestras reales anonimizadas y confirmar qué campos exporta la instalación utilizada de Assetto Corsa.
3. **Backend:** decidir estrategia de JWT y su almacenamiento en cliente, implementar roles y pertenencia por liga, y aportar casos de prueba.
4. **Datos:** revisar modelos SQL y entidades JPA frente a muestras reales y validar migraciones.
5. **Integración:** fijar versiones reproducibles de JDK/Node/MySQL, revisar versiones y vulnerabilidades de dependencias, automatizar pruebas y establecer el criterio de PR.

**Conclusión documental:** los cambios de configuración están registrados y comparados con el estado inicial; los riesgos pendientes se distinguen expresamente de los corregidos. No se afirma que la aplicación esté funcional o certificada como segura.
