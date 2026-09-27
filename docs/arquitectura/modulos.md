# RaceManager — Arquitectura y Módulos

**Actualizado:** 27/09/2026 — alcance de la segunda entrega y prioridades del MVP; refleja el importador integrado en `main`.
**Estados:** ✅ implementado · 🟡 parcial · ⚪ pendiente · ➖ fuera del MVP. **Prioridades:** P0 = indispensable para demostrar el flujo MVP; P1 = complementario en versión mínima (por ejemplo, precarga de equipos/circuitos e historial); P2 = ampliación posterior al MVP. Las prioridades son propuestas de planificación y deben validarse con el equipo y el tutor.

## Arquitectura general

```text
┌─────────────────────┐
│      FRONTEND       │  React + Vite
│  (módulos UI/rol)   │
└──────────┬──────────┘
           │ HTTP / REST (/api)
           ▼
┌─────────────────────┐
│       BACKEND       │  Spring Boot + Security + JPA
│   (módulos dominio) │
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│      DATABASE       │  MySQL 8
│  database/schema.sql│
└─────────────────────┘

Assetto Corsa (archivos oficiales)
        │
        │ carga manual del Manager
        ▼
  Módulo Importación → API RaceManager

Seguridad: JWT en el encabezado Authorization; roles + pertenencia a la liga.
Ver docs/seguridad/seguridad.md
```

## Estructura del repositorio

```text
racemanager/
├── README.md
├── database/
│   ├── schema.sql              # Esquema MySQL canónico
│   ├── migraciones/            # Cambios para bases existentes
│   └── datos-desarrollo.sql    # Datos de prueba (solo desarrollo)
├── docs/
│   ├── propuesta/
│   ├── mvp/                    # Alcance y criterios de aceptación
│   ├── seguridad/
│   ├── importacion/
│   ├── db/
│   │   └── modelo-datos.md
│   └── arquitectura/
│       └── modulos.md          # Este documento
├── backend/                    # API Spring Boot
└── frontend/                   # App React + Vite
```

## Módulos Backend (`backend/`)

Paquete base: `com.racemanager.api`. **P0** significa obligatorio para el flujo demostrable; no implica implementar todas las funciones posibles del módulo.

| Módulo | Paquete | Responsabilidad en el alcance planificado | Prioridad | Estado actual |
|---|---|---|---|---|
| Auth | `auth` | Login JWT, BCrypt y validación de sesión | P0 | ✅ Login, JWT y `/me`; recuperación y registro público fuera del MVP |
| Usuario | `usuario` | Identidades, roles y cuentas necesarias para el flujo | P0 | 🟡 Entidades, roles y repositorios; alta administrativa pendiente |
| Liga | `liga` | Alta, consulta y autorización por pertenencia | P0 | 🟡 Consulta y aislamiento implementados; alta pendiente |
| Equipo | `equipo` | Equipo mínimo asociado a cada piloto | P1 | ⚪ Precarga necesaria por FK; ABM completo fuera del MVP |
| Piloto | `piloto` | Alta, asociación a equipo/liga y participación en carrera | P0 | ⚪ Sin endpoints ni persistencia del módulo |
| Vehículo | `vehiculo` | Catálogo y asociaciones avanzadas de vehículos | P2 | ➖ Fuera del MVP; referencia opcional de vehículo ya existe en participantes |
| Circuito | `circuito` | Catálogo mínimo para asignación a carrera | P1 | ⚪ Tabla lista; precarga pendiente |
| Carrera | `carrera` | Alta de carrera, participantes y estado de publicación | P0 | 🟡 Entidad/repositorio para importación; altas y publicación pendientes |
| Sesión | `sesion` | Confirmación de sesión, clasificación oficial y vueltas | P0 | ⚪ Esquema disponible; servicio de confirmación pendiente |
| Importación | `importacion` | JSON nativo AC, validación, vista previa y carga sin duplicados | P0 | 🟡 Parser, carga y vista previa persistida; faltan asociación de pilotos y confirmación |
| Estadística | `estadistica` | Historial básico de piloto y consulta de resultados | P1 | ⚪ Historial básico pendiente; analítica avanzada fuera del MVP |
| Common / Config | `common`, `config` | Seguridad, respuestas de error y healthcheck | P0 | ✅ Funciones principales implementadas |

## Módulos Frontend (`frontend/`)

| Módulo | Carpeta | Responsabilidad en el alcance planificado | Prioridad | Estado actual |
|---|---|---|---|---|
| Landing | `src/modules/landing` | Presentación pública y navegación | P1 | 🟡 Vista inicial disponible; diseño final pendiente |
| Auth | `src/modules/auth` | Formulario de acceso y gestión de sesión | P0 | ✅ Login integrado con API |
| Ligas | `src/modules/ligas` | Alta/consulta de ligas propias | P0 | ⚪ Carpeta de módulo inicial |
| Equipos | `src/modules/equipos` | Selección/precarga del equipo mínimo | P1 | ⚪ Carpeta de módulo inicial |
| Pilotos | `src/modules/pilotos` | Alta y vinculación de pilotos a la carrera | P0 | ⚪ Carpeta de módulo inicial |
| Carreras | `src/modules/carreras` | Crear carrera, cargar archivo, revisar y publicar resultados | P0 | ⚪ Carpeta de módulo inicial |
| Estadísticas | `src/modules/estadisticas` | Vista de resultados publicados e historial básico | P1 | ⚪ Carpeta de módulo inicial |
| Services | `src/services` | Cliente HTTP, JWT y almacenamiento de sesión | P0 | ✅ Axios y sesión disponibles |

### Orden de implementación para el MVP

1. **P0:** alta de liga, pilotos y carrera (criterio CA-03); en paralelo, vistas React con contrato de API acordado.
2. **P0:** completar importación, asociación de pilotos, revisión, confirmación transaccional y publicación (CA-04 a CA-06).
3. **P1 mínimo:** consulta de resultados publicados, historial básico y catálogos mínimos de equipos/circuitos (CA-07).
4. **Calidad transversal:** pruebas negativas por rol y liga, integración MySQL y una demo accesible (CA-08). Los módulos P2 se posponen.

## Base de datos

- Script: [`database/schema.sql`](../../database/schema.sql)
- Documentación: [`docs/db/modelo-datos.md`](../db/modelo-datos.md)
