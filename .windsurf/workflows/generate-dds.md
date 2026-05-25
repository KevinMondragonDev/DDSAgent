---
description: Genera el DDS Santander completo (JSON + MD + DOCX) para un proyecto colocado en projects/<nombre>/. Invoca este workflow con /generate-dds y pasa el nombre del proyecto.
---

# Workflow `/generate-dds`

Pipeline reproducible end-to-end para producir el Documento de Diseño del Sistema en formato Santander a partir de un repositorio.

## Entradas requeridas

- `<proyecto>`: nombre del subdirectorio dentro de `projects/` (kebab-case recomendado).
- El código fuente del proyecto ya copiado/clonado en `projects/<proyecto>/`.

## Pasos

### 1. Reconocer el proyecto

Ejecuta el reconocimiento siguiendo `.windsurf/skills/generate-dds-santander/analysis-guide.md` sección **0. Reconocimiento**. Construye un inventario en `todo_list`.

### 2. Análisis profundo

Cubre las secciones 1–11 de `analysis-guide.md`. Para cada bloque, registra la evidencia (rutas relativas al repo).

### 3. Producir `agente.json` (+ `cuestionario.md` si hay lagunas)

Crea el archivo `data/<proyecto>/agente.json` siguiendo el esqueleto de `.windsurf/skills/generate-dds-santander/tags-catalog.md`. Reglas:

- **Si una sección no tiene evidencia, NO inventes.** Pon `"PENDIENTE_CUESTIONARIO"` y añade una entrada en `data/<proyecto>/cuestionario.md` siguiendo `.windsurf/skills/generate-dds-santander/questionnaire-format.md`. También añade el resumen a `pendientes[]`.
- Idioma español neutro técnico.
- `metadata.autor` = `"Windsurf AI"`.
- `metadata.fecha_actual` y `metadata.fecha_version_documento` = fecha de hoy en `DD/MM/YYYY`.

### 4. Validar el JSON
// turbo
```powershell
Get-Content 'data/<proyecto>/agente.json' -Raw | ConvertFrom-Json | Out-Null; Write-Host 'OK'
```

Si falla, corrige el JSON antes de continuar.

### 5. Decidir camino según haya o no cuestionario
// turbo
```powershell
$sentinels = Select-String -Pattern "PENDIENTE_CUESTIONARIO" -Path "data/<proyecto>/agente.json" -SimpleMatch
if ($sentinels) {
  Write-Host "⚠️  Hay $($sentinels.Count) lagunas. Revisa data/<proyecto>/cuestionario.md y responde. Luego invoca /answer-questionnaire <proyecto>."
  Write-Host "   Si quieres ver el DDS borrador con sentinels visibles, continúa con los pasos 6–7. NO entregues así."
} else {
  Write-Host "✅ Sin lagunas. Procediendo a renderizar entregable final."
}
```

**Si hay cuestionario pendiente:** detente aquí, avísale al usuario y espera. El usuario responderá y luego invocará `/answer-questionnaire <proyecto>` que cierra el ciclo.

**Si NO hay cuestionario:** continúa con los siguientes pasos.

### 6. Compilar el renderizador (solo la primera vez)
// turbo
```powershell
cd tools
mvn package
```

### 7. Renderizar Documentos (Markdown y Word)
// turbo
```powershell
java -jar tools/target/dds-tools.jar <proyecto>
```

Salida esperada: `output/<proyecto>/DDS_<proyecto>.md` y `output/<proyecto>/DDS_<proyecto>.docx`. El script debe imprimir `Todos los placeholders han sido sustituidos correctamente.`. Si imprime errores, abrir issue y NO entregar.

### 9. Verificación visual

Abre el `.docx` en Word/LibreOffice. Comprueba:

- Tabla "Vocabulario común y acrónimos" expandida a N filas (no 6).
- Encabezados de Control de Ediciones / Versiones con fechas correctas.
- Funcionalidades F-01..F-NN presentes.
- Footer con la fecha del documento.
- **Cero apariciones de `PENDIENTE_CUESTIONARIO`** en el cuerpo del documento.

### 10. Entregar

Comparte con el usuario:

- `output/<proyecto>/DDS_<proyecto>.docx` (entregable principal).
- `output/<proyecto>/DDS_<proyecto>.md` (versión revisable en VSCode).
- `data/<proyecto>/agente.json` (fuente de verdad versionable).

## Iteración

Si el usuario solicita cambios sobre el `.docx`:

1. **No** edites el `.docx` directamente. Es un artefacto generado.
2. Actualiza `data/<proyecto>/agente.json`.
3. Vuelve a ejecutar los pasos 6 y 7.

## Cuándo NO usar este workflow

- Si el usuario quiere SOLO la versión Markdown: usa solo el paso 6.
- Si el usuario quiere SOLO actualizar metadata (versión, fecha, descripción del cambio): edita `data/<proyecto>/agente.json` y re-render.
- Si el proyecto es trivial (un único script): probablemente el DDS Santander está sobreingenierizado; consulta con el usuario antes.
