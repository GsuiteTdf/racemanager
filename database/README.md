# Base de datos RaceManager

Esquema MySQL 8 (requiere 8.0.16 o superior por las restricciones `CHECK`).

| Archivo | Descripción |
|---|---|
| `schema.sql` | Creación de la base, tablas, claves foráneas y roles iniciales (versión 2). |
| `migraciones/002_importacion_y_publicacion.sql` | Actualiza una base creada con la versión 1 del esquema. |
| `pruebas/verificar-restricciones.sql` | Prueba automática de las restricciones (duplicados, una confirmada por carrera, publicación auditada). Solo sobre bases de prueba; la ejecuta el CI. |
| `datos-desarrollo.sql` | Usuarios y ligas de ejemplo **solo para desarrollo** (contraseña documentada en el archivo). |

Documentación del modelo: [`docs/db/modelo-datos.md`](../docs/db/modelo-datos.md)

## Base nueva

```bash
mysql -u root -p < database/schema.sql
mysql -u root -p < database/datos-desarrollo.sql   # opcional, solo desarrollo
```

## Base existente creada con la versión 1

```bash
mysql -u root -p < database/migraciones/002_importacion_y_publicacion.sql
```

## Usuario de aplicación

No conectar la API con `root`. Crear un usuario con permisos solo sobre la base:

```sql
CREATE USER 'racemanager'@'localhost' IDENTIFIED BY '<contraseña-local>';
GRANT SELECT, INSERT, UPDATE, DELETE ON racemanager.* TO 'racemanager'@'localhost';
```

La contraseña se pasa a la API mediante la variable de entorno `DB_PASSWORD`.
