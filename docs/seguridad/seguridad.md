# RaceManager — Seguridad

> Estado y trazabilidad de cada hallazgo: [`../auditoria/diagnostico-y-trazabilidad-2026-09-26.md`](../auditoria/diagnostico-y-trazabilidad-2026-09-26.md).

Estado de la configuración de seguridad y reglas para los próximos módulos.

## 1. Qué cambió en esta entrega

| Antes | Ahora |
|---|---|
| `anyRequest().permitAll()`: todos los endpoints abiertos | Públicos solo `GET /api/health` y `POST /api/auth/login`; el resto de `/api/**` exige JWT; cualquier otra ruta se deniega |
| `httpBasic` activo sin usuarios reales | Desactivado (también formLogin y logout de sesión) |
| Sin autenticación | Login con BCrypt y JWT HS256 con expiración, emisor y audiencia verificados; contraseñas de más de 72 bytes rechazadas |
| Sin control por liga | `@ligaAccess`: un Manager solo accede a sus propias ligas |
| Usuario `root` sin clave en `application.properties` | `DB_URL`, `DB_USER`, `DB_PASSWORD` por variables de entorno |
| — | `JWT_SECRET` obligatorio (≥ 32 caracteres); la API no arranca sin él |
| Errores con detalle interno | Respuestas `401`/`403`/`400` en JSON, sin mensajes ni trazas internas |
| Java 24 (sin soporte) | Java 21 LTS (Gradle obtiene el JDK si falta) |
| Nada impedía olvidar `@PreAuthorize` | `EndpointsProtegidosTest` falla si un endpoint nuevo no declara la regla |
| Claves fijas en pruebas y CI | Valores aleatorios por ejecución; escaneo de secretos sin hallazgos |

## 2. Cómo funciona

```text
Cliente ── POST /api/auth/login {email, password} ──► AuthService
                                                        │ BCrypt + usuario activo
        ◄──────────── { token, expiraEn, usuario } ─────┘
Cliente ── GET /api/ligas  Authorization: Bearer <token> ──► JwtAuthenticationFilter
                                                               │ firma, vencimiento, emisor
                                                               ▼
                                          @PreAuthorize (rol) + @ligaAccess (pertenencia)
```

- **Roles** en el token: `ADMIN`, `MANAGER`, `EQUIPO`, `PILOTO` (→ `ROLE_*` en Spring Security).
  El frontend los usa solo para mostrar u ocultar opciones; **el backend nunca confía en datos
  de rol enviados por el cliente**.
- **Pertenencia a liga:** `LigaAccessGuard.puedeGestionar(authentication, ligaId)`:
  ADMIN → sí; MANAGER → solo si `liga.manager_id` es su id; otros roles → no.
  Una liga inexistente responde igual que una ajena (`403`) para no revelar ids.

## 3. Reglas obligatorias para endpoints nuevos

1. Todo endpoint de gestión lleva `@PreAuthorize`. Nada de `permitAll()` salvo consultas públicas
   explícitas de carreras **publicadas**.
2. Si recibe un `ligaId`, usar `@PreAuthorize("@ligaAccess.puedeGestionar(authentication, #ligaId)")`.
3. Si recibe el id de un recurso de una liga (carrera, piloto, equipo, importación), **obtener la
   liga del recurso en el servidor** y verificar pertenencia. No aceptar un `ligaId` del cliente
   como prueba de pertenencia de otro recurso.
4. Al asociar entidades (ej.: piloto a carrera) verificar que ambas pertenecen a la misma liga:
   una FK válida de otra liga no debe habilitar acceso cruzado.
5. Las consultas para pilotos y públicas filtran `carrera.estado = 'PUBLICADA'` **en el backend**.
6. Cada endpoint suma pruebas: anónimo (401), rol incorrecto (403), Manager de otra liga (403) y caso válido.
7. No agregar un `@ExceptionHandler(Exception.class)` genérico: ocultaría los `403`.

## 4. Importación de archivos (iteración 3)

- Límite de 2 MB por archivo (`spring.servlet.multipart.*`); validar además extensión, tipo y estructura.
- Calcular SHA-256 del contenido y rechazar repetidos (`UNIQUE (carrera_id, hash_sha256)`).
- Procesar en una transacción: si algo falla, no queda ningún resultado parcial.
- No guardar el archivo con el nombre enviado por el usuario (evita *path traversal*); usar un nombre generado.
- Si los archivos se conservan en el servidor, analizarlos con antivirus antes de procesarlos (aporte de la 1ra entrega).
- Publicar solo después de la confirmación explícita del Manager; la base impide publicar sin auditoría.

## 5. Configuración local

Variables requeridas (ver `backend/.env.example`). Spring Boot **no** lee archivos `.env`:
definirlas en la configuración de ejecución de IntelliJ o en la terminal.

```powershell
# PowerShell (Windows) — solo para la sesión actual
$env:DB_USER="racemanager"; $env:DB_PASSWORD="..."; $env:JWT_SECRET="<48+ caracteres aleatorios>"
cd backend; .\gradlew.bat bootRun
```

Generar un secreto: `openssl rand -base64 48` (o cualquier generador de 48+ caracteres aleatorios).

## 6. Pendientes conocidos

| Pendiente | Prioridad | Nota |
|---|---|---|
| Ejecutar las pruebas del backend (32 pruebas escritas) | **Alta** | No pudieron ejecutarse por falta de acceso a Maven; corren en el CI del PR |
| Limitar intentos de login (fuerza bruta) | Media | Antes del despliegue público |
| Alta de usuarios y cambio de contraseña | Media | Definir quién crea Managers (decisión abierta 3) |
| Revocación de tokens / usuarios desactivados | Media | Hoy un token sigue válido hasta vencer (60 min por defecto) |
| HTTPS y secretos en la plataforma de despliegue | Alta al desplegar | Nunca publicar `JWT_SECRET` ni `DB_PASSWORD` |
| Documentación OpenAPI con esquema Bearer | Media | Parte de CA-08 |
| `ddl-auto=validate` contra MySQL | Media | Al completar el mapeo de entidades |
