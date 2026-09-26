> **Documento histórico — 1ra entrega.** Describía la política transitoria (todo denegado) previa a JWT. Reemplazado por [`../../seguridad/seguridad.md`](../../seguridad/seguridad.md) y [`../../mvp/alcance-mvp.md`](../../mvp/alcance-mvp.md). Sus reglas se incorporaron allí; ver la matriz de trazabilidad.

# Seguridad y ejecución del MVP — estado y tareas

Para el diagnóstico previo, evidencia antes/después y estado de cada hallazgo, ver [auditoría y trazabilidad de cambios](../diagnostico-y-trazabilidad-2026-09-26.md).

## Estado de esta entrega

- **Corregido:** se elimina el `permitAll()` global. Solo `GET /api/health` es público;
  los demás endpoints quedan **denegados por defecto**. Esta medida es transitoria
  y evita presentar una autenticación ficticia como terminada.
- **Corregido:** credenciales y URL de MySQL configurables con `DB_USER`, `DB_PASSWORD`
  y `DB_URL`. No publicar contraseñas reales ni archivos `.env`.
- **Agregado:** límites iniciales de tamaño de carga; **no sustituyen** la validación
  de tipo, contenido ni antivirus cuando se implemente el importador.
- **Pendiente:** autenticación JWT real, roles y pertenencia a liga, parser, controles
  de duplicación, transacciones, pruebas de integración y frontend conectado.

## Reglas obligatorias al implementar JWT

1. `POST /api/auth/login`: validar email y hash BCrypt, devolver JWT firmado con
   expiración corta; almacenar secreto fuera de Git; rechazar usuarios inactivos.
2. Proteger `/api/**` salvo health y login. Comprobar firma, expiración, emisor y
   audiencia; no confiar en el rol recibido desde el frontend.
3. Las operaciones de Manager requieren rol MANAGER **y** pertenencia a la liga
   consultada: el `liga.manager_id` debe coincidir con el usuario autenticado.
4. Consultas públicas deben filtrar `carrera.estado = PUBLICADA` en el backend.
5. No aceptar IDs de liga arbitrarios sin comprobación de pertenencia; evitar
   que una FK válida de otra liga habilite acceso cruzado.
6. Antes de publicar: revisión explícita por el Manager; operaciones de importación
   atómicas y política documentada para reintentos y duplicados.
7. Probar acceso anónimo, JWT inválido/expirado, rol incorrecto, liga ajena,
   carrera no publicada, carga inválida y duplicada.

## Importación — contrato pendiente de muestras

Solicitar al organizador dos o más archivos reales anonimizados. Registrar nombre,
formato, tamaño, campos observados, codificación y casos especiales. Recién entonces
fijar DTO, parser y reglas de validación. Evitar afirmar compatibilidad sin muestras.

## Nota de integración

No se han modificado las relaciones SQL existentes ni se han implementado endpoints
ficticios. El SQL es el esquema inicial sujeto a verificación con archivos reales.
Las pruebas de seguridad y la ejecución integral siguen pendientes; requieren un entorno MySQL configurado.
