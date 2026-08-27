# Trabajo Final de Grado

## Integrantes

- Diego Alejandro Velardes - email: velardesdiego@gmail.com
- Claudio Rodriguez - email: rodriguezop8@gmail.com
- Gaston Cejas - email: gaston.cejas@gmail.com

## Tutor

Juan Ignacio Schiavonni

---

# RaceManager — Gestión Integral de Ligas y Competencias de Simracing

## Descripción del proyecto

El proyecto consiste en el desarrollo de una plataforma web para la gestión integral de ligas, equipos, pilotos y competencias de **simracing**, con foco inicial en el simulador **Assetto Corsa**.

El sistema permitirá centralizar la información de ligas, equipos, pilotos, vehículos, circuitos y carreras desde una única plataforma. La carga de resultados y datos de cada competencia estará a cargo del **Manager/Organizador** de la liga o del evento, quien es la fuente oficial de la información. A partir de esa carga, los equipos y pilotos podrán consultar posteriormente su rendimiento, historial y estadísticas.

La aplicación estará desarrollada bajo una arquitectura cliente-servidor, utilizando una API REST como intermediaria entre el frontend y el backend. El sistema contará con diferentes roles de usuario y mecanismos de autenticación y autorización para garantizar que cada usuario acceda únicamente a las funcionalidades y datos correspondientes a sus permisos.

El proyecto estará diseñado considerando la posibilidad de ser utilizado por diferentes ligas o comunidades de simracing, manteniendo aislada la información correspondiente a cada organización.

Como parte del proyecto se desarrollará también la documentación de la API y se realizará el despliegue de los componentes principales en servicios disponibles en la nube, permitiendo disponer de una versión funcional accesible desde Internet.

## Problemática

La gestión de resultados y datos de competencias de simracing suele resolverse hoy mediante herramientas no pensadas para ese fin: planillas de cálculo, mensajes de Discord, archivos sueltos generados por el simulador o servidores de carrera, y registros manuales llevados por los organizadores de las ligas.

Esto genera diferentes inconvenientes:

- Resultados dispersos entre distintos canales (Discord, planillas, archivos locales).
- Dificultad para mantener un historial completo de las carreras de una liga.
- Falta de centralización de la información de equipos, pilotos y vehículos.
- Dificultad para consultar el rendimiento histórico de un piloto a lo largo de una temporada.
- Falta de estadísticas centralizadas de carreras y sesiones.
- Dificultad para comparar diferentes sesiones y vueltas de forma ordenada.
- Dependencia de la buena voluntad y disponibilidad del organizador para compartir resultados.
- Falta de una fuente única y confiable de datos oficiales de la competencia.
- Falta de herramientas específicas, orientadas a comunidades de simracing, para centralizar y analizar esta información.

El sistema propuesto busca resolver esto centralizando en una única plataforma la carga oficial de resultados por parte del organizador de la liga, y permitiendo que equipos y pilotos consulten esa información de forma ordenada, histórica y comparable.

## Objetivo general

Desarrollar una plataforma web para la gestión integral de ligas de simracing que permita administrar usuarios, equipos, pilotos, vehículos, circuitos, carreras y sesiones, mediante una arquitectura basada en una API REST, incorporando la carga de datos oficiales por parte del organizador y herramientas para el análisis del rendimiento de pilotos y equipos.

## Objetivos específicos

- Diseñar e implementar una API REST para la gestión de las funcionalidades principales del sistema.
- Implementar un sistema de autenticación mediante JWT.
- Implementar autorización basada en roles y permisos.
- Permitir la creación y administración de ligas o competencias.
- Permitir la creación y administración de equipos.
- Permitir registrar y administrar pilotos asociados a equipos.
- Permitir registrar y administrar vehículos (autos del simulador).
- Permitir registrar circuitos utilizados en las competencias.
- Gestionar calendarios y eventos de carreras dentro de una liga.
- Permitir que el organizador cargue los archivos oficiales generados por Assetto Corsa al finalizar una carrera.
- Procesar los archivos cargados para extraer resultados, vueltas y tiempos.
- Mantener un historial deportivo de cada piloto y equipo.
- Permitir consultar estadísticas de rendimiento por piloto, equipo y circuito.
- Permitir comparar diferentes sesiones y vueltas.
- Implementar un flujo de publicación de resultados (carga → procesamiento → publicación).
- Desarrollar diferentes interfaces según el tipo de usuario.
- Implementar una base de datos para almacenar la información del sistema.
- Documentar la API para facilitar su utilización e integración.
- Desplegar al menos uno de los componentes principales del sistema en un servicio online.

## Alcance del proyecto

### Funcionalidades principales

#### Gestión de usuarios

El sistema contará con diferentes roles de usuario:

- Administrador.
- Manager / Organizador de liga.
- Equipo.
- Piloto.

Cada rol contará con diferentes permisos y alcance de datos.

#### Autenticación y autorización

Se implementará:

- Registro de usuarios.
- Inicio de sesión.
- Autenticación mediante JWT.
- Protección de endpoints.
- Autorización basada en roles.
- Cierre de sesión.
- Gestión segura de contraseñas.

#### Gestión de ligas / competencias

Los usuarios con rol Manager podrán administrar su propia liga o competencia.

La plataforma permitirá:

- Registrar una liga o competencia.
- Definir categorías dentro de la liga.
- Registrar el calendario de carreras de la temporada.
- Administrar equipos participantes.
- Administrar pilotos participantes.
- Consultar estadísticas generales de la liga.

Cada liga mantiene su información aislada del resto de las ligas registradas en la plataforma.

#### Gestión de equipos

El sistema permitirá administrar la información de los equipos que participan en una liga.

Cada equipo podrá contar con:

- Nombre.
- Liga a la que pertenece.
- Pilotos asociados.
- Vehículos asociados.
- Historial de carreras.
- Estadísticas del equipo.

#### Gestión de pilotos

El sistema permitirá administrar la información de los pilotos (sim-racers).

Cada piloto podrá contar con información como:

- Nombre / Nickname.
- Equipo.
- Categoría.
- Vehículo asociado.
- Historial de carreras.
- Historial de sesiones.
- Resultados.
- Estadísticas de rendimiento.

La plataforma permitirá consultar la evolución histórica de cada piloto a lo largo de la temporada.

#### Gestión de vehículos

El sistema permitirá registrar los vehículos (autos del simulador) utilizados por los equipos y pilotos.

Cada vehículo podrá contar con:

- Nombre / modelo (según catálogo de Assetto Corsa).
- Categoría.
- Equipo asociado.
- Piloto asociado.
- Historial de utilización en carreras y sesiones.

#### Gestión de circuitos

La plataforma permitirá administrar los circuitos utilizados en las competencias, correspondientes a los circuitos disponibles en Assetto Corsa.

Cada circuito podrá contar con:

- Nombre.
- Longitud.
- Cantidad de curvas.
- Categoría o liga asociada.
- Información adicional.

#### Gestión de carreras

Las carreras serán una de las funcionalidades principales del sistema.

Cada carrera tendrá información como:

- Liga / categoría.
- Circuito.
- Fecha.
- Equipos participantes.
- Pilotos participantes.
- Resultados por piloto.
- Estado.

Los posibles estados podrán ser:

- Programada.
- Cargada (pendiente de publicación).
- Publicada.
- Cancelada.

#### Carga de datos por parte del Manager

El **Manager/Organizador** es el único responsable de cargar los datos oficiales de una carrera. Los equipos y pilotos no cargan resultados: únicamente los consultan una vez publicados.

Flujo principal:

```
ORGANIZADOR / MANAGER
        │
        │  Carga los archivos generados
        │  por Assetto Corsa al finalizar
        │  la carrera
        ▼
   RaceManager
        │
        ├── Procesa los archivos
        ├── Valida la información
        └── Genera resultados y estadísticas
        │
        ▼
   Estado: Cargada
        │
        │  El Manager revisa y confirma
        ▼
   Estado: Publicada
        │
        ▼
Equipos y pilotos acceden a los datos
```

Mientras una carrera está en estado "Cargada", solo el Manager puede visualizarla y corregirla. Al publicarla, pasa a estar disponible para los equipos y pilotos correspondientes.

El sistema contemplará la importación y el procesamiento de archivos generados por Assetto Corsa al finalizar una sesión o carrera, permitiendo incorporar esa información al historial de cada piloto y equipo. El alcance exacto de los datos a extraer (vueltas, sectores, tiempos, posiciones, velocidad, u otros disponibles) se terminará de definir durante la etapa de análisis, en base a archivos reales de competencias.

#### Gestión de sesiones y vueltas

Cada carrera podrá contener una o más sesiones (por ejemplo, clasificación y carrera), y cada sesión podrá contener múltiples vueltas por piloto.

Cada vuelta podrá registrar, según la información disponible en los archivos procesados:

- Número de vuelta.
- Tiempo total.
- Tiempos de sector (si están disponibles).
- Estado de la vuelta (válida/inválida).
- Sesión asociada.

Esto permitirá identificar la mejor vuelta de una sesión y comparar diferentes vueltas realizadas en el mismo circuito.

#### Análisis de rendimiento

El sistema contará con herramientas para consultar el rendimiento de pilotos y equipos.

Se podrán obtener estadísticas como:

- Mejor tiempo de vuelta.
- Tiempo promedio.
- Cantidad de vueltas y sesiones.
- Evolución de tiempos a lo largo de la temporada.
- Consistencia de vueltas.
- Resultados históricos por circuito.

#### Comparación de sesiones

El sistema permitirá comparar diferentes sesiones realizadas por un piloto, o sesiones entre distintos pilotos de un mismo equipo, para analizar su evolución dentro de la liga.

#### Panel de administración

El sistema contará con paneles adaptados a cada tipo de usuario.

**Administrador**

Podrá:
- Gestionar usuarios.
- Gestionar ligas.
- Administrar roles.
- Consultar información general del sistema.

**Manager / Organizador**

Podrá:
- Crear y administrar su liga.
- Registrar el calendario de carreras.
- Administrar equipos y pilotos participantes.
- Cargar los archivos oficiales de cada carrera.
- Revisar y publicar resultados.
- Consultar estadísticas generales de la liga.

**Equipo**

Podrá:
- Administrar la información básica de su equipo.
- Consultar sus pilotos y vehículos.
- Consultar carreras publicadas.
- Consultar estadísticas y resultados de sus pilotos.

**Piloto**

Podrá:
- Consultar su información.
- Consultar sus carreras y sesiones publicadas.
- Consultar sus tiempos y vueltas.
- Consultar sus estadísticas personales.
- Comparar sus propias sesiones.

#### Estadísticas y Dashboard

El sistema contará con un dashboard para visualizar información relevante, adaptado según el rol del usuario.

Se podrán mostrar, entre otros:

- Cantidad de pilotos, equipos y carreras de la liga.
- Mejores tiempos de la temporada.
- Evolución de tiempos por piloto.
- Resultados históricos por circuito.

Los datos podrán representarse mediante gráficos y tablas para facilitar su interpretación.

### Funcionalidades complementarias

Estas funcionalidades podrán incorporarse durante el desarrollo si el tiempo y los recursos disponibles lo permiten:

- Gráficos comparativos de vueltas.
- Análisis de sectores.
- Comparación entre pilotos de distintos equipos.
- Exportación de estadísticas.
- Generación de reportes.
- Notificaciones a equipos/pilotos al publicarse una carrera.
- Registro de auditoría de operaciones del Manager.
- Soporte para otras categorías o campeonatos dentro de una misma liga.
- Integración futura con otros simuladores.

Estas funcionalidades tendrán prioridad secundaria frente al núcleo principal: gestión de ligas, equipos, pilotos, carreras y carga/publicación de resultados.

## Stack tecnológico

### Backend

| Tecnología | Uso |
|---|---|
| Java | Lenguaje de programación |
| Spring Boot | Framework de desarrollo |
| Spring Security | Autenticación y autorización |
| Spring Data JPA | Persistencia de datos |
| MySQL | Base de datos relacional |
| JWT | Autenticación basada en tokens |
| Gradle | Gestión de dependencias y construcción |

### Frontend

| Tecnología | Uso |
|---|---|
| React | Desarrollo de la interfaz |
| Vite.js | Herramienta de construcción y servidor de desarrollo |
| React Router | Navegación del lado del cliente |
| Axios | Comunicación con la API REST |
| Bootstrap | Diseño y estilos responsive |
| Font Awesome | Iconografía |

### Integración y procesamiento de datos

| Tecnología | Uso |
|---|---|
| Assetto Corsa | Fuente de los archivos oficiales de carrera |
| Archivos de resultados / sesión | Origen de los datos a procesar |
| JSON | Intercambio de información |
| REST API | Comunicación entre componentes |

El formato concreto de los archivos a procesar, y el alcance exacto de los datos a extraer, se definirán durante la etapa de análisis, en base a archivos reales provistos por una competencia de simracing.

### Herramientas de desarrollo

| Herramienta | Uso |
|---|---|
| IntelliJ IDEA | Desarrollo del backend |
| MySQL Workbench | Administración de la base de datos |
| Postman | Pruebas de la API |
| Git | Control de versiones |
| GitHub | Repositorio y colaboración |
| Visual Studio Code | Desarrollo del frontend |

## Arquitectura general

El sistema seguirá una arquitectura cliente-servidor.

```
                    ┌─────────────────────┐
                    │      FRONTEND       │
                    │       React         │
                    └──────────┬──────────┘
                               │
                               │ HTTP / REST
                               ▼
                    ┌─────────────────────┐
                    │       BACKEND       │
                    │    Spring Boot      │
                    │    Spring Security  │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │      DATABASE       │
                    │        MySQL        │
                    └─────────────────────┘


             ┌───────────────────────────────┐
             │       ASSETTO CORSA           │
             │  Archivos oficiales de una    │
             │  carrera (resultados/sesión)  │
             └───────────────┬───────────────┘
                             │
                     Carga manual por
                     el Manager/Organizador
                             │
                             ▼
                  ┌──────────────────────┐
                  │  Importador / Parser  │
                  └──────────┬───────────┘
                             │
                             ▼
                       RaceManager API
```

La carga de archivos se realiza siempre a través del Manager/Organizador desde la plataforma web, sin requerir conexión directa ni en tiempo real con el simulador.

## Modelo conceptual inicial

Las principales entidades del sistema serán:

```
Usuario
   │
   ├── Liga
   │      │
   │      ├── Equipo
   │      │      │
   │      │      ├── Piloto
   │      │      │      │
   │      │      │      └── Resultados / Sesiones
   │      │      │             │
   │      │      │             └── Vueltas
   │      │      │
   │      │      └── Vehículo
   │      │
   │      └── Carrera
   │             │
   │             └── Circuito
   │
   └── Roles
```

La estructura definitiva de entidades y relaciones será definida durante la etapa de análisis y diseño del sistema, una vez analizados archivos reales de competencias de simracing.

## Roadmap inicial

### Etapa 1 — Análisis y diseño
- Definición del alcance.
- Identificación de usuarios y roles.
- Diseño de casos de uso.
- Diseño de arquitectura.
- Diseño inicial de base de datos.
- Definición de API REST.
- Análisis de archivos reales de Assetto Corsa para definir el alcance de la importación.

### Etapa 2 — Backend
- Configuración del proyecto Spring Boot.
- Implementación de entidades.
- Implementación de repositorios.
- Implementación de servicios.
- Implementación de controladores.
- Implementación de autenticación.
- Implementación de autorización.
- Documentación de endpoints.

### Etapa 3 — Frontend
- Configuración de React.
- Implementación del login.
- Implementación de gestión de usuarios.
- Implementación de gestión de ligas y equipos.
- Implementación de pilotos y vehículos.
- Implementación de carreras.
- Implementación del dashboard.

### Etapa 4 — Carga y procesamiento de datos de Assetto Corsa
- Análisis de los archivos disponibles.
- Definición del formato de datos a procesar.
- Desarrollo del componente de importación.
- Integración con la API.
- Almacenamiento de resultados, sesiones y vueltas.
- Validación de datos cargados.
- Flujo de publicación de carreras.

### Etapa 5 — Análisis
- Estadísticas de pilotos y equipos.
- Comparación de vueltas.
- Comparación de sesiones.
- Estadísticas por circuito.
- Gráficos de rendimiento.

### Etapa 6 — Pruebas y despliegue
- Pruebas unitarias.
- Pruebas de integración.
- Pruebas de API.
- Pruebas funcionales.
- Corrección de errores.
- Configuración del entorno de producción.
- Despliegue.
- Documentación final.

## Alcance del MVP

Para garantizar la viabilidad del proyecto dentro del período académico, el producto mínimo viable contemplará:

- Registro e inicio de sesión.
- Roles de usuario (Administrador, Manager, Equipo, Piloto).
- Gestión de ligas.
- Gestión de equipos.
- Gestión de pilotos.
- Gestión de vehículos.
- Gestión de circuitos.
- Gestión de carreras.
- Carga de archivos oficiales por parte del Manager.
- Procesamiento e importación de resultados básicos (vueltas y tiempos).
- Flujo de publicación de carreras (Cargada → Publicada).
- Consulta de estadísticas básicas.
- Dashboard.
- API REST documentada.
- Base de datos.
- Despliegue online.

El alcance exacto de los datos importados desde Assetto Corsa se ajustará una vez analizados archivos reales de competencias, priorizando inicialmente resultados, vueltas y tiempos por sobre datos más avanzados de telemetría.

## Posibilidades de transferencia

RaceManager podrá ser utilizado como base para una solución destinada a:

- Ligas de simracing.
- Comunidades de Assetto Corsa.
- Organizadores de campeonatos de simracing.
- Equipos y pilotos independientes de simracing.

El sistema podrá evolucionar posteriormente hacia una plataforma especializada en análisis de rendimiento y gestión deportiva de simracing, incorporando nuevas fuentes de datos, otros simuladores y funcionalidades de análisis más avanzadas e incluso obtencion de datos de manera online.
