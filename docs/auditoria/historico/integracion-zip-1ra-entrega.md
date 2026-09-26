> **Documento histórico — 1ra entrega.** Instrucciones para incorporar un ZIP sin `.git`. Reemplazado por [`../../integracion/guia-integracion.md`](../../integracion/guia-integracion.md).

# Cómo incorporar esta entrega sin pisar el trabajo del equipo

Esta entrega es una **copia limpia de los archivos fuente del ZIP del compañero**,
sin `.git`, `node_modules`, `build`, `dist` ni cachés. No equivale a un commit ni
incluye el historial remoto. **No reemplazar** la carpeta `.git` de tu clon.

1. En tu clon original: `git status` y guardar o confirmar cualquier cambio previo.
2. Crear una branch nueva desde `main` actualizado, por ejemplo
   `git switch main; git pull origin main; git switch -c feature/mvp-seguridad-base`.
3. Comparar esta entrega con el clon usando VS Code; incorporar archivos fuente
   conscientemente. Si tu branch `docs/refinamiento-mvp` ya tiene trabajo, hacer
   primero commit de ese trabajo y resolver diferencias antes de integrar.
4. Revisar `git diff`, especialmente la propuesta, `SecurityConfig.java`,
   `application.properties` y `.gitignore`.
5. Ejecutar pruebas con MySQL configurado; **no publicar ni desplegar** hasta
   completar JWT y controles de pertenencia.
6. `git add` solo los archivos deseados, `git commit`, `git push -u origin ...`,
   abrir PR para revisión del equipo. No subir archivos `.env` ni credenciales.

No se realizó un merge automático contra el GitHub remoto actual.
