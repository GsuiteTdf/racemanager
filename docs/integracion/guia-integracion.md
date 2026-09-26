# Guía de integración — de las dos entregas al Pull Request

Cómo se consolidaron la 1ra entrega (seguridad transitoria + auditoría) y la 2da entrega
(implementación), y cómo revisar y proponer los cambios al equipo sin afectar el repositorio de nadie.
Reemplaza a [`../auditoria/historico/integracion-zip-1ra-entrega.md`](../auditoria/historico/integracion-zip-1ra-entrega.md).

## 1. Repositorios y remotos

| Remoto | URL | Uso |
|---|---|---|
| `origin` | `https://github.com/GsuiteTdf/racemanager.git` | Fork de Gastón. **Único destino de push.** |
| `upstream` | `https://github.com/ClauRodriguez/racemanager.git` | Repositorio del equipo. Solo lectura: fetch permitido, **push deshabilitado**. |

Configuración aplicada en el clon de trabajo:

```bash
git remote set-url origin https://github.com/GsuiteTdf/racemanager.git
git remote add upstream https://github.com/ClauRodriguez/racemanager.git
git remote set-url --push upstream NO_PUSH_repo_original_de_Claudio   # evita pushes accidentales
git remote -v
```

## 2. Qué se tomó de cada entrega

| Origen | Se incorporó | No se incorporó (y por qué) |
|---|---|---|
| 1ra entrega | Diagnóstico inicial (conservado sin cambios en `docs/auditoria/historico/` y ampliado en la versión consolidada); reglas de `seguridad-y-mvp.md` (audiencia del JWT, rechazo de inactivos, filtro de `PUBLICADA`, pruebas negativas, antivirus en el importador); guía de ZIP como documento histórico. | `SecurityConfig` con `denyAll()` para todo: habría desactivado el login ya implementado. Su versión de la propuesta: eliminaba la viabilidad y los riesgos que ya estaban en GitHub. Clases marcadoras (`AuthModule`, etc.) ya reemplazadas por código real. |
| 2da entrega | Los 7 commits completos (autenticación, restricciones SQL, frontend, MVP, CI). | — |
| Consolidación | Correcciones de auditoría (`AUD-01`…`AUD-11`), pruebas nuevas, evidencia de ejecución, README y documentación actualizados. | — |

## 3. Historial de la branch

```text
127a38d  (upstream/main, origin/main)  propuesta con viabilidad y riesgos
  └─ 5792797 … 46ed017   7 commits de la 2da entrega (sin cambios)
      └─ commits de consolidación (correcciones, pruebas, documentación)
```

Respaldo del estado previo a la consolidación: branch local `respaldo/2da-entrega-original` y bundle
`respaldos/2da-entrega-original-46ed017.bundle` (fuera del repositorio).

## 4. Revisar antes de proponer al equipo

```bash
git fetch upstream
git log --oneline --decorate upstream/main..feature/mejoras-mvp-2da-entrega
git diff --stat upstream/main...feature/mejoras-mvp-2da-entrega
git diff upstream/main...feature/mejoras-mvp-2da-entrega -- docs/propuesta   # contenido del equipo conservado
```

Si `upstream/main` avanzó: `git merge upstream/main` (o `git rebase upstream/main` si la branch todavía
no se compartió), resolviendo conflictos **conservando ambas contribuciones**. Nunca `push --force`.

## 5. Publicar en el fork y abrir el PR interno

```bash
git push -u origin feature/mejoras-mvp-2da-entrega
```

PR: `GsuiteTdf/racemanager` → base `main`. Mantenerlo en **Draft** hasta que el CI del backend pase.
En un fork, GitHub Actions viene deshabilitado: habilitarlo en la pestaña **Actions** del fork.

## 6. Proponer al repositorio del equipo (paso posterior, manual)

Solo cuando Gastón haya revisado todo y el CI esté en verde: abrir desde GitHub un PR
`GsuiteTdf:feature/mejoras-mvp-2da-entrega` → `ClauRodriguez:main` y pedir revisión a Diego y Claudio.
No hacer push ni merge directo sobre el repositorio del equipo.
