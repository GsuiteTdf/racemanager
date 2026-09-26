-- =============================================================================
-- RaceManager — Prueba automática de las restricciones de integridad (MySQL 8)
-- Ejecutar sobre una base de PRUEBA recién creada con schema.sql (lo hace el CI).
-- Termina con error (SIGNAL) si alguna regla no se cumple; si todo está bien
-- muestra "OK: restricciones verificadas".
-- Nunca ejecutar sobre una base con datos reales: inserta y borra filas de prueba.
-- =============================================================================

USE racemanager;

DROP PROCEDURE IF EXISTS rm_verificar_restricciones;

DELIMITER //
CREATE PROCEDURE rm_verificar_restricciones()
BEGIN
  DECLARE rechazado INT DEFAULT 0;
  DECLARE v_usuario BIGINT UNSIGNED;
  DECLARE v_liga BIGINT UNSIGNED;
  DECLARE v_circuito BIGINT UNSIGNED;
  DECLARE v_carrera BIGINT UNSIGNED;
  DECLARE CONTINUE HANDLER FOR 1062, 3819 SET rechazado = 1;  -- clave duplicada / CHECK violado

  START TRANSACTION;

  INSERT INTO usuario (email, password_hash, nombre) VALUES ('prueba.restricciones@test.local', 'x', 'Prueba');
  SET v_usuario = LAST_INSERT_ID();
  INSERT INTO liga (nombre, manager_id) VALUES ('Liga prueba restricciones', v_usuario);
  SET v_liga = LAST_INSERT_ID();
  INSERT INTO circuito (nombre) VALUES ('Circuito prueba restricciones');
  SET v_circuito = LAST_INSERT_ID();
  INSERT INTO carrera (liga_id, circuito_id, nombre, fecha_hora) VALUES (v_liga, v_circuito, 'R1', NOW(3));
  SET v_carrera = LAST_INSERT_ID();

  -- 1) El mismo archivo (mismo hash) no puede cargarse dos veces para la misma carrera
  INSERT INTO importacion_carrera (carrera_id, manager_id, nombre_archivo, hash_sha256, tamano_bytes, detalle_json)
    VALUES (v_carrera, v_usuario, 'a.json', REPEAT('a', 64), 10, '{"fuente":"AC_SERVER_JSON"}');
  SET rechazado = 0;
  INSERT INTO importacion_carrera (carrera_id, manager_id, nombre_archivo, hash_sha256, tamano_bytes)
    VALUES (v_carrera, v_usuario, 'copia.json', REPEAT('a', 64), 10);
  IF rechazado = 0 THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'FALLA: se aceptó un archivo duplicado para la misma carrera';
  END IF;

  -- 2) Solo una importación CONFIRMADA por carrera
  INSERT INTO importacion_carrera (carrera_id, manager_id, nombre_archivo, hash_sha256, tamano_bytes, estado)
    VALUES (v_carrera, v_usuario, 'b.json', REPEAT('b', 64), 10, 'CONFIRMADA');
  SET rechazado = 0;
  INSERT INTO importacion_carrera (carrera_id, manager_id, nombre_archivo, hash_sha256, tamano_bytes, estado)
    VALUES (v_carrera, v_usuario, 'c.json', REPEAT('c', 64), 10, 'CONFIRMADA');
  IF rechazado = 0 THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'FALLA: se aceptaron dos importaciones confirmadas';
  END IF;

  -- 3) Varias RECHAZADAS sí están permitidas
  SET rechazado = 0;
  INSERT INTO importacion_carrera (carrera_id, manager_id, nombre_archivo, hash_sha256, tamano_bytes, estado)
    VALUES (v_carrera, v_usuario, 'd.json', REPEAT('d', 64), 10, 'RECHAZADA'),
           (v_carrera, v_usuario, 'e.json', REPEAT('e', 64), 10, 'RECHAZADA');
  IF rechazado = 1 THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'FALLA: se rechazaron importaciones RECHAZADAS válidas';
  END IF;

  -- 4) No se puede publicar sin registrar quién y cuándo
  SET rechazado = 0;
  UPDATE carrera SET estado = 'PUBLICADA' WHERE id = v_carrera;
  IF rechazado = 0 THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'FALLA: se publicó una carrera sin auditoría';
  END IF;

  -- 5) Con auditoría, la publicación es válida
  SET rechazado = 0;
  UPDATE carrera SET estado = 'PUBLICADA', publicada_en = NOW(3), publicada_por = v_usuario WHERE id = v_carrera;
  IF rechazado = 1 THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'FALLA: se rechazó una publicación válida';
  END IF;

  ROLLBACK;  -- no deja datos de prueba
  SELECT 'OK: restricciones verificadas' AS resultado;
END //
DELIMITER ;

CALL rm_verificar_restricciones();
DROP PROCEDURE rm_verificar_restricciones;
