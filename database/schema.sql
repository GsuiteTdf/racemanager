-- =============================================================================
-- RaceManager — Esquema de Base de Datos (MySQL)
-- Versión 2 (2da entrega): importación sin duplicados y publicación auditada.
-- Para bases creadas con la versión 1 aplicar database/migraciones/002_*.sql
-- =============================================================================
-- Motor: MySQL 8.x
-- Charset: utf8mb4
-- =============================================================================

CREATE DATABASE IF NOT EXISTS racemanager
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE racemanager;

-- -----------------------------------------------------------------------------
-- Roles y usuarios
-- -----------------------------------------------------------------------------

CREATE TABLE rol (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  codigo      VARCHAR(32)  NOT NULL,  -- ADMIN, MANAGER, EQUIPO, PILOTO
  nombre      VARCHAR(80)  NOT NULL,
  descripcion VARCHAR(255) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_rol_codigo (codigo)
) ENGINE=InnoDB;

CREATE TABLE usuario (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  email           VARCHAR(180) NOT NULL,
  password_hash   VARCHAR(255) NOT NULL,
  nombre          VARCHAR(120) NOT NULL,
  nickname        VARCHAR(80)  NULL,
  activo          TINYINT(1)   NOT NULL DEFAULT 1,
  creado_en       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  actualizado_en  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_usuario_email (email)
) ENGINE=InnoDB;

CREATE TABLE usuario_rol (
  usuario_id BIGINT UNSIGNED NOT NULL,
  rol_id     BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (usuario_id, rol_id),
  CONSTRAINT fk_usuario_rol_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
  CONSTRAINT fk_usuario_rol_rol     FOREIGN KEY (rol_id)     REFERENCES rol (id)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- Ligas y categorías
-- -----------------------------------------------------------------------------

CREATE TABLE liga (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  nombre          VARCHAR(150) NOT NULL,
  descripcion     TEXT         NULL,
  temporada       VARCHAR(40)  NULL,  -- ej. "2024", "2025-S1"
  manager_id      BIGINT UNSIGNED NOT NULL,
  activa          TINYINT(1)   NOT NULL DEFAULT 1,
  creado_en       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  actualizado_en  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY ix_liga_manager (manager_id),
  CONSTRAINT fk_liga_manager FOREIGN KEY (manager_id) REFERENCES usuario (id)
) ENGINE=InnoDB;

CREATE TABLE categoria (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  liga_id     BIGINT UNSIGNED NOT NULL,
  nombre      VARCHAR(100) NOT NULL,
  descripcion VARCHAR(255) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_categoria_liga_nombre (liga_id, nombre),
  CONSTRAINT fk_categoria_liga FOREIGN KEY (liga_id) REFERENCES liga (id)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- Equipos, pilotos y vehículos
-- -----------------------------------------------------------------------------

CREATE TABLE equipo (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  liga_id         BIGINT UNSIGNED NOT NULL,
  nombre          VARCHAR(120) NOT NULL,
  usuario_id      BIGINT UNSIGNED NULL,  -- cuenta con rol EQUIPO (opcional)
  logo_url        VARCHAR(500) NULL,
  activo          TINYINT(1)   NOT NULL DEFAULT 1,
  creado_en       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  actualizado_en  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_equipo_liga_nombre (liga_id, nombre),
  KEY ix_equipo_usuario (usuario_id),
  CONSTRAINT fk_equipo_liga    FOREIGN KEY (liga_id)    REFERENCES liga (id),
  CONSTRAINT fk_equipo_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
) ENGINE=InnoDB;

CREATE TABLE piloto (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  equipo_id       BIGINT UNSIGNED NOT NULL,
  usuario_id      BIGINT UNSIGNED NULL,  -- cuenta con rol PILOTO (opcional)
  categoria_id    BIGINT UNSIGNED NULL,
  nombre          VARCHAR(120) NOT NULL,
  nickname        VARCHAR(80)  NOT NULL,
  activo          TINYINT(1)   NOT NULL DEFAULT 1,
  creado_en       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  actualizado_en  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_piloto_equipo_nickname (equipo_id, nickname),
  KEY ix_piloto_usuario (usuario_id),
  KEY ix_piloto_categoria (categoria_id),
  CONSTRAINT fk_piloto_equipo    FOREIGN KEY (equipo_id)    REFERENCES equipo (id),
  CONSTRAINT fk_piloto_usuario   FOREIGN KEY (usuario_id)   REFERENCES usuario (id),
  CONSTRAINT fk_piloto_categoria FOREIGN KEY (categoria_id) REFERENCES categoria (id)
) ENGINE=InnoDB;

CREATE TABLE vehiculo (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  liga_id         BIGINT UNSIGNED NOT NULL,
  equipo_id       BIGINT UNSIGNED NULL,
  piloto_id       BIGINT UNSIGNED NULL,
  categoria_id    BIGINT UNSIGNED NULL,
  nombre_modelo   VARCHAR(150) NOT NULL,  -- catálogo Assetto Corsa
  activo          TINYINT(1)   NOT NULL DEFAULT 1,
  creado_en       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  actualizado_en  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY ix_vehiculo_liga (liga_id),
  KEY ix_vehiculo_equipo (equipo_id),
  KEY ix_vehiculo_piloto (piloto_id),
  CONSTRAINT fk_vehiculo_liga      FOREIGN KEY (liga_id)      REFERENCES liga (id),
  CONSTRAINT fk_vehiculo_equipo    FOREIGN KEY (equipo_id)    REFERENCES equipo (id),
  CONSTRAINT fk_vehiculo_piloto    FOREIGN KEY (piloto_id)    REFERENCES piloto (id),
  CONSTRAINT fk_vehiculo_categoria FOREIGN KEY (categoria_id) REFERENCES categoria (id)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- Circuitos
-- -----------------------------------------------------------------------------

CREATE TABLE circuito (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  nombre           VARCHAR(150) NOT NULL,
  pais             VARCHAR(80)  NULL,
  longitud_metros  INT UNSIGNED NULL,
  cantidad_curvas  SMALLINT UNSIGNED NULL,
  descripcion      VARCHAR(500) NULL,
  activo           TINYINT(1)   NOT NULL DEFAULT 1,
  creado_en        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_circuito_nombre (nombre)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- Carreras, participantes y flujo de publicación
-- Estados: PROGRAMADA | CARGADA | PUBLICADA | CANCELADA
-- -----------------------------------------------------------------------------

CREATE TABLE carrera (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  liga_id         BIGINT UNSIGNED NOT NULL,
  categoria_id    BIGINT UNSIGNED NULL,
  circuito_id     BIGINT UNSIGNED NOT NULL,
  nombre          VARCHAR(150) NOT NULL,  -- ej. "Ronda 2"
  fecha_hora      DATETIME(3)  NOT NULL,
  estado          ENUM('PROGRAMADA', 'CARGADA', 'PUBLICADA', 'CANCELADA')
                  NOT NULL DEFAULT 'PROGRAMADA',
  notas           TEXT         NULL,
  publicada_en    DATETIME(3)  NULL,      -- momento de la publicación (auditoría)
  publicada_por   BIGINT UNSIGNED NULL,   -- Manager/Admin que publicó
  creado_en       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  actualizado_en  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY ix_carrera_liga_fecha (liga_id, fecha_hora),
  KEY ix_carrera_estado (estado),
  CONSTRAINT fk_carrera_liga      FOREIGN KEY (liga_id)      REFERENCES liga (id),
  CONSTRAINT fk_carrera_categoria FOREIGN KEY (categoria_id) REFERENCES categoria (id),
  CONSTRAINT fk_carrera_circuito  FOREIGN KEY (circuito_id)  REFERENCES circuito (id),
  CONSTRAINT fk_carrera_publicada_por FOREIGN KEY (publicada_por) REFERENCES usuario (id),
  -- Una carrera PUBLICADA siempre registra cuándo y quién la publicó
  CONSTRAINT ck_carrera_publicacion CHECK (
    estado <> 'PUBLICADA' OR (publicada_en IS NOT NULL AND publicada_por IS NOT NULL))
) ENGINE=InnoDB;

CREATE TABLE carrera_equipo (
  carrera_id BIGINT UNSIGNED NOT NULL,
  equipo_id  BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (carrera_id, equipo_id),
  CONSTRAINT fk_carrera_equipo_carrera FOREIGN KEY (carrera_id) REFERENCES carrera (id),
  CONSTRAINT fk_carrera_equipo_equipo  FOREIGN KEY (equipo_id)  REFERENCES equipo (id)
) ENGINE=InnoDB;

CREATE TABLE carrera_piloto (
  carrera_id  BIGINT UNSIGNED NOT NULL,
  piloto_id   BIGINT UNSIGNED NOT NULL,
  vehiculo_id BIGINT UNSIGNED NULL,
  PRIMARY KEY (carrera_id, piloto_id),
  KEY ix_carrera_piloto_vehiculo (vehiculo_id),
  CONSTRAINT fk_carrera_piloto_carrera  FOREIGN KEY (carrera_id)  REFERENCES carrera (id),
  CONSTRAINT fk_carrera_piloto_piloto   FOREIGN KEY (piloto_id)   REFERENCES piloto (id),
  CONSTRAINT fk_carrera_piloto_vehiculo FOREIGN KEY (vehiculo_id) REFERENCES vehiculo (id)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- Sesiones, resultados y vueltas
-- -----------------------------------------------------------------------------

CREATE TABLE sesion (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  carrera_id  BIGINT UNSIGNED NOT NULL,
  tipo        ENUM('PRACTICA', 'CLASIFICACION', 'CARRERA') NOT NULL,
  nombre      VARCHAR(100) NOT NULL,
  orden       SMALLINT UNSIGNED NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uk_sesion_carrera_orden (carrera_id, orden),
  CONSTRAINT fk_sesion_carrera FOREIGN KEY (carrera_id) REFERENCES carrera (id)
) ENGINE=InnoDB;

CREATE TABLE resultado_sesion (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  sesion_id        BIGINT UNSIGNED NOT NULL,
  piloto_id        BIGINT UNSIGNED NOT NULL,
  posicion         SMALLINT UNSIGNED NULL,
  vueltas_completadas SMALLINT UNSIGNED NULL,
  tiempo_total_ms  INT UNSIGNED NULL,
  mejor_vuelta_ms  INT UNSIGNED NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_resultado_sesion_piloto (sesion_id, piloto_id),
  KEY ix_resultado_piloto (piloto_id),
  CONSTRAINT fk_resultado_sesion FOREIGN KEY (sesion_id) REFERENCES sesion (id),
  CONSTRAINT fk_resultado_piloto FOREIGN KEY (piloto_id) REFERENCES piloto (id)
) ENGINE=InnoDB;

CREATE TABLE vuelta (
  id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  resultado_sesion_id BIGINT UNSIGNED NOT NULL,
  numero_vuelta       SMALLINT UNSIGNED NOT NULL,
  tiempo_total_ms     INT UNSIGNED NOT NULL,
  sector1_ms          INT UNSIGNED NULL,
  sector2_ms          INT UNSIGNED NULL,
  sector3_ms          INT UNSIGNED NULL,
  valida              TINYINT(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uk_vuelta_resultado_numero (resultado_sesion_id, numero_vuelta),
  CONSTRAINT fk_vuelta_resultado FOREIGN KEY (resultado_sesion_id) REFERENCES resultado_sesion (id)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- Importación de archivos Assetto Corsa (auditoría de carga del Manager)
-- -----------------------------------------------------------------------------

CREATE TABLE importacion_carrera (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  carrera_id      BIGINT UNSIGNED NOT NULL,
  manager_id      BIGINT UNSIGNED NOT NULL,
  nombre_archivo  VARCHAR(255) NOT NULL,
  ruta_almacenada VARCHAR(500) NULL,
  hash_sha256     CHAR(64)     NOT NULL,  -- huella del contenido: detecta archivos repetidos
  tamano_bytes    INT UNSIGNED NOT NULL,
  -- PENDIENTE: recibido | PROCESADA: datos extraídos, en revisión del Manager
  -- CONFIRMADA: resultados aceptados | RECHAZADA: descartada por el Manager | ERROR: archivo inválido
  estado          ENUM('PENDIENTE', 'PROCESADA', 'CONFIRMADA', 'RECHAZADA', 'ERROR')
                  NOT NULL DEFAULT 'PENDIENTE',
  mensaje_error   TEXT NULL,
  detalle_json    LONGTEXT NULL, -- datos normalizados en revisión; sin GUID externos
  cargado_en      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  procesado_en    DATETIME(3) NULL,
  revisado_por    BIGINT UNSIGNED NULL,
  revisado_en     DATETIME(3) NULL,
  -- Columna calculada: vale carrera_id solo si la importación está CONFIRMADA
  carrera_confirmada_id BIGINT UNSIGNED
                  AS (IF(estado = 'CONFIRMADA', carrera_id, NULL)) STORED,
  PRIMARY KEY (id),
  KEY ix_importacion_carrera (carrera_id),
  -- El mismo archivo no puede cargarse dos veces para la misma carrera
  UNIQUE KEY uk_importacion_carrera_hash (carrera_id, hash_sha256),
  -- Como máximo una importación CONFIRMADA por carrera (evita resultados duplicados)
  UNIQUE KEY uk_importacion_una_confirmada (carrera_confirmada_id),
  CONSTRAINT fk_importacion_carrera FOREIGN KEY (carrera_id) REFERENCES carrera (id),
  CONSTRAINT fk_importacion_manager FOREIGN KEY (manager_id) REFERENCES usuario (id),
  CONSTRAINT fk_importacion_revisor FOREIGN KEY (revisado_por) REFERENCES usuario (id)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- Datos iniciales: roles
-- -----------------------------------------------------------------------------

INSERT INTO rol (codigo, nombre, descripcion) VALUES
  ('ADMIN',   'Administrador',        'Administración general de la plataforma'),
  ('MANAGER', 'Manager / Organizador','Crea y administra ligas, carga y publica resultados'),
  ('EQUIPO',  'Equipo',               'Consulta y administra información básica del equipo'),
  ('PILOTO',  'Piloto',               'Consulta resultados, vueltas y estadísticas propias');
