-- Aplicar DESPUÉS de la migración 002 y ANTES de arrancar el importador.
-- No se almacenan archivos originales ni identificadores externos del simulador.
USE racemanager;
ALTER TABLE importacion_carrera ADD COLUMN detalle_json LONGTEXT NULL;
