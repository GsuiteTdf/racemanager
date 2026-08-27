# 🏁 RaceManager

### Gestión Integral de Ligas y Competencias de Simracing

**Trabajo Final**

---

## 👥 Integrantes

* **Diego Alejandro Velardes**
* **Claudio Rodriguez**
* **Gaston Cejas**

### 👨‍🏫 Tutor

**Juan Ignacio Schiavonni**

---

## 📌 Descripción

**RaceManager** es una plataforma web propuesta para la gestión integral de ligas y competencias de **simracing**, con foco inicial en **Assetto Corsa**.

El sistema busca centralizar la información de ligas, equipos, pilotos, vehículos, circuitos y carreras, permitiendo que los organizadores carguen los datos oficiales de las competencias y que posteriormente equipos y pilotos puedan consultar sus resultados y estadísticas.

---

## 🎯 Objetivo

Desarrollar una plataforma web que permita gestionar competencias de simracing y centralizar sus resultados, incorporando herramientas para consultar y analizar el rendimiento de pilotos y equipos.

---

## 🛠️ Tecnologías

### Backend

* Java
* Spring Boot
* Spring Security
* Spring Data JPA
* JWT
* Gradle

### Frontend

* React
* Vite
* React Router
* Axios
* Bootstrap

### Base de datos

* MySQL

### Integración

* Assetto Corsa
* REST API
* JSON

### Herramientas

* Git / GitHub
* IntelliJ IDEA
* Visual Studio Code
* MySQL Workbench
* Postman

---

## 🏗️ Arquitectura propuesta

El sistema utilizará una arquitectura cliente-servidor:

```text
┌──────────────┐
│   Frontend   │
│    React     │
└──────┬───────┘
       │ REST API
       ▼
┌──────────────┐
│   Backend    │
│ Spring Boot  │
└──────┬───────┘
       │
       ▼
┌──────────────┐
│    MySQL     │
└──────────────┘

Assetto Corsa
      │
      │ Archivos de resultados
      ▼
RaceManager API
```

---

## 🚀 Roadmap

| Etapa | Descripción                             | Estado      |
| ----- | --------------------------------------- | ----------- |
| 1     | Análisis y diseño                       | 🟡 Actual   |
| 2     | Desarrollo Backend                      | ⚪ Pendiente |
| 3     | Desarrollo Frontend                     | ⚪ Pendiente |
| 4     | Procesamiento de datos de Assetto Corsa | ⚪ Pendiente |
| 5     | Estadísticas y análisis                 | ⚪ Pendiente |
| 6     | Pruebas y despliegue                    | ⚪ Pendiente |

---

## 📁 Estructura del repositorio (posible estructura)

```text
racemanager/
│
├── README.md
├── .gitignore
│
├── docs/
│   ├── propuesta/
│   │   └── propuesta-proyecto.md
│   │
│   └── entregas/
│       └── entrega-01/
│           └── propuesta.md
│
├── backend/
├── frontend/
└── database/
```

---

## 📚 Documentación

La documentación completa de la propuesta y las diferentes entregas se encuentra dentro de la carpeta:

```text
docs/
```

La propuesta completa del proyecto puede consultarse en:

`docs/propuesta/propuesta-proyecto.md`

---

## 📌 Estado del proyecto

🟡 **Propuesta / Análisis inicial**

El proyecto se encuentra actualmente en etapa de propuesta.

El desarrollo del software todavía no ha comenzado. El repositorio será utilizado como espacio único para centralizar el código fuente, documentación, base de datos y demás componentes del proyecto.

---

## 🔗 Repositorio

Este repositorio constituye el **repositorio único de GitHub** del proyecto RaceManager.

**URL:** `https://github.com/ClauRodriguez/racemanager`
