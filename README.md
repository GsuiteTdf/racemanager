# RaceManager

Gestión integral de ligas y competencias de simracing (foco inicial: Assetto Corsa).

**Trabajo Final** — Diego Alejandro Velardes · Claudio Rodriguez · Gastón Cejas<br>
**Tutor:** Juan Ignacio Schiavonni

## Estado real del proyecto

| Área | Estado |
|---|---|
| Seguridad (login BCrypt + JWT, roles, aislamiento por liga) | 🟦 Implementada; 32 pruebas del backend escritas, **pendientes de ejecución** en CI |
| Restricciones MySQL (duplicados, una importación confirmada, publicación auditada) | ✅ Verificadas en MySQL 8 (base nueva y migrada) |
| Frontend: login conectado a la API y manejo de sesión | ✅ 7 pruebas Vitest, build y `npm audit` OK |
| Gestión de ligas, pilotos y carreras | ⬜ Solo consulta de ligas; altas pendientes (iteración 2) |
| Importación de archivos de Assetto Corsa | ⬜ Pendiente: faltan muestras reales para definir el formato |
| Revisión, publicación y consulta de resultados | ⬜ Pendiente (la base ya impide publicar sin auditoría) |

**El MVP todavía no está terminado:** el circuito carrera → importación → publicación → consulta no está
implementado. Detalle en la [auditoría y trazabilidad](docs/auditoria/diagnostico-y-trazabilidad-2026-09-26.md),
el [alcance del MVP](docs/mvp/alcance-mvp.md) y los [criterios de aceptación](docs/mvp/criterios-aceptacion.md).

---

## Descripción

Plataforma web para centralizar ligas, pilotos, circuitos y carreras. El Manager/Organizador carga
los resultados oficiales a partir de los archivos de Assetto Corsa, los revisa y los publica;
equipos y pilotos consultan los resultados publicados.

## Arquitectura

```text
Frontend (React + Vite)  ──HTTP/REST + JWT──►  API (Spring Boot)  ──►  MySQL 8
                                                   ▲
                               Archivos de resultados de Assetto Corsa (carga del Manager)
```

Detalle: [`docs/arquitectura/modulos.md`](docs/arquitectura/modulos.md)

## Tecnologías y versiones

| Capa | Stack | Versión requerida |
|---|---|---|
| Backend | Java, Spring Boot 4, Spring Security, Spring Data JPA, JWT (jjwt), Gradle | **JDK 21** |
| Frontend | React 19, TypeScript, Vite, React Router, Axios, Bootstrap | **Node 20+** |
| Base de datos | MySQL | **8.0.16+** |

**Herramientas de desarrollo:** Git / GitHub · IntelliJ IDEA · Visual Studio Code · MySQL Workbench · Postman

## Puesta en marcha

### 1. Base de datos

```bash
mysql -u root -p < database/schema.sql
mysql -u root -p < database/datos-desarrollo.sql   # opcional: usuarios y ligas de prueba
```

Crear un usuario de aplicación (no usar `root`): ver [`database/README.md`](database/README.md).

### 2. Backend

Definir las variables de entorno (referencia: [`backend/.env.example`](backend/.env.example)).
Spring Boot **no** lee archivos `.env`: configurarlas en IntelliJ (*Run Configuration → Environment
variables*) o en la terminal.

```powershell
# Windows PowerShell
$env:DB_USER="racemanager"; $env:DB_PASSWORD="<clave-local>"
$env:JWT_SECRET="<al menos 32 caracteres aleatorios>"
cd backend
.\gradlew.bat bootRun
```

```bash
# Linux / macOS
export DB_USER=racemanager DB_PASSWORD='<clave-local>' JWT_SECRET='<al menos 32 caracteres aleatorios>'
cd backend && ./gradlew bootRun
```

Si falta `JWT_SECRET` (o tiene menos de 32 caracteres) la API no arranca, a propósito.

Pruebas automáticas (usan H2 en memoria, no necesitan MySQL; si falta el JDK 21, Gradle lo descarga):

```bash
cd backend && ./gradlew test          # Windows: .\gradlew.bat test
```

Prueba de las restricciones de la base (solo sobre una base de prueba):

```bash
mysql -u root -p < database/pruebas/verificar-restricciones.sql
```

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

App: `http://localhost:5173` (el proxy de Vite redirige `/api` al backend en `:8080`).

Pruebas del frontend: `npm test`.

## Endpoints disponibles

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| GET | `/api/health` | Público | Estado del servicio |
| POST | `/api/auth/login` | Público | Devuelve un JWT |
| GET | `/api/auth/me` | Autenticado | Identidad del token |
| GET | `/api/ligas` | ADMIN, MANAGER | ADMIN: todas; MANAGER: solo las propias |
| GET | `/api/ligas/{ligaId}` | ADMIN o Manager de esa liga | Detalle de la liga |

Todo lo demás requiere autenticación o está denegado por defecto. Ver [`docs/seguridad/seguridad.md`](docs/seguridad/seguridad.md).

## Roadmap

| Etapa | Descripción | Estado |
|---|---|---|
| 1 | Análisis y diseño | 🟡 En curso: MVP delimitado; faltan relevamiento y muestras reales |
| 2 | Desarrollo Backend | 🟡 Seguridad y consulta de ligas implementadas |
| 3 | Desarrollo Frontend | 🟡 Login conectado a la API |
| 4 | Procesamiento de datos de Assetto Corsa | ⚪ Pendiente (restricciones SQL listas) |
| 5 | Estadísticas y análisis | ⚪ Fuera del MVP, salvo historial básico |
| 6 | Pruebas y despliegue | 🟡 CI configurado; despliegue pendiente |

Plan detallado por iteraciones: [`docs/mvp/alcance-mvp.md`](docs/mvp/alcance-mvp.md#6-plan-incremental).

## Estructura del repositorio

```text
racemanager/
├── backend/           # API Spring Boot (paquetes por módulo de dominio)
├── frontend/          # App React + Vite (módulos de UI)
├── database/          # schema.sql, migraciones y datos de desarrollo
├── docs/              # propuesta, MVP, seguridad, auditoría, integración, arquitectura, modelo de datos
├── .github/           # CI y plantilla de Pull Request
└── CONTRIBUTING.md    # flujo de trabajo con branches y Pull Requests
```

## Documentación

| Documento | Ruta |
|---|---|
| Propuesta | [`docs/propuesta/propuesta-proyecto.md`](docs/propuesta/propuesta-proyecto.md) |
| Alcance del MVP | [`docs/mvp/alcance-mvp.md`](docs/mvp/alcance-mvp.md) |
| Criterios de aceptación y definición de terminado | [`docs/mvp/criterios-aceptacion.md`](docs/mvp/criterios-aceptacion.md) |
| Seguridad | [`docs/seguridad/seguridad.md`](docs/seguridad/seguridad.md) |
| **Auditoría: diagnóstico, correcciones y matriz de trazabilidad** | [`docs/auditoria/diagnostico-y-trazabilidad-2026-09-26.md`](docs/auditoria/diagnostico-y-trazabilidad-2026-09-26.md) |
| Evidencias de pruebas ejecutadas | [`docs/auditoria/evidencias/`](docs/auditoria/evidencias/) |
| Documentos históricos de la 1ra entrega | [`docs/auditoria/historico/`](docs/auditoria/historico/) |
| **Guía de integración** (remotos, branches, PR) | [`docs/integracion/guia-integracion.md`](docs/integracion/guia-integracion.md) |
| Relevamiento de archivos Assetto Corsa | [`docs/importacion/relevamiento-archivos.md`](docs/importacion/relevamiento-archivos.md) |
| Arquitectura y módulos | [`docs/arquitectura/modulos.md`](docs/arquitectura/modulos.md) |
| Modelo de datos | [`docs/db/modelo-datos.md`](docs/db/modelo-datos.md) |
| Cómo contribuir | [`CONTRIBUTING.md`](CONTRIBUTING.md) |

## Repositorios

- Equipo: https://github.com/ClauRodriguez/racemanager
- Fork de trabajo (Gastón): https://github.com/GsuiteTdf/racemanager — ver la [guía de integración](docs/integracion/guia-integracion.md).
