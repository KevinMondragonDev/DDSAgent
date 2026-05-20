---
description: Reabsorbe las respuestas del usuario en data/<proyecto>/cuestionario.md, actualiza agente.json reemplazando los sentinels PENDIENTE_CUESTIONARIO y re-renderiza los entregables. Invoca con /answer-questionnaire.
---

# Workflow `/answer-questionnaire`

Cierra el ciclo del cuestionario: el usuario respondió `data/<proyecto>/cuestionario.md`, este workflow incorpora las respuestas en `agente.json` y vuelve a renderizar.

## Entradas

- `<proyecto>`: nombre del subdirectorio dentro de `data/`.
- Archivo: `data/<proyecto>/cuestionario.md` con respuestas del usuario.

## Pasos

### 1. Verificar que existe el cuestionario

```powershell
if (-Not (Test-Path "data/<proyecto>/cuestionario.md")) {
  Write-Host "❌ No hay cuestionario para <proyecto>. ¿Querías /generate-dds en su lugar?"
  exit 1
}
```

### 2. Parsear el cuestionario

Lee `data/<proyecto>/cuestionario.md` y extrae para cada bloque `### Q-<NN>`:

- **`Clave JSON`** (línea con `- **Clave JSON:** `…``).
- **Bloque de respuesta** (el primer fenced code block ` ```text ` debajo de `**Respuesta:**`).
- **Tipo esperado** (para validar el shape).

Reglas:

- Si el bloque de respuesta está **vacío** o solo tiene `<el usuario llena este bloque>` literal → la pregunta sigue pendiente, NO se actualiza el JSON para ese campo.
- Si tiene contenido → se considera respondida.

### 3. Actualizar `data/<proyecto>/agente.json`

Para cada pregunta respondida:

1. Localizar el campo en `agente.json` usando la `Clave JSON` (formato `path.to.field` con soporte para `[i]` y `[*]`).
2. Si el valor actual es `"PENDIENTE_CUESTIONARIO"` (o un objeto/array que lo contenga), reemplazarlo por la respuesta parseada según el `Tipo esperado`:
   - `string` → texto literal sin la línea de bloque ` ``` `.
   - `string[]` → una entrada por línea no vacía.
   - `{...}` o `Funcionalidad[]` → parsear como JSON; si falla, **NO** actualizar y reportar al usuario.
3. Eliminar la entrada correspondiente de `pendientes[]` del JSON.

### 4. Validar que el JSON sigue parseando
// turbo
```powershell
node -e "JSON.parse(require('fs').readFileSync('data/<proyecto>/agente.json','utf8'));console.log('OK')"
```

### 5. Marcar el cuestionario

Re-escribe `data/<proyecto>/cuestionario.md`:

- Para cada pregunta respondida, mover el bloque a una sección `## Histórico (respondidas)` al final del archivo.
- Para cada pregunta aún pendiente, conservarla en su sección original.
- Actualizar el contador `Preguntas respondidas: M/N` en la cabecera.

Si **no quedan preguntas pendientes**, renombrar el archivo a `cuestionario.completed.md` (señal de que todo está cerrado).

### 6. Re-renderizar
// turbo
```powershell
node tools/render-md.js   <proyecto>
node tools/render-docx.js <proyecto>
```

### 7. Reportar al usuario

Resumen claro:

```
✅ Respuestas incorporadas: M de N
⚠️  Pendientes: <N - M>  (ver data/<proyecto>/cuestionario.md)
✅ DDS regenerado en output/<proyecto>/
```

Si quedan pendientes, listar las claves restantes para que el usuario priorice.

## Errores comunes

| Síntoma | Causa | Acción |
|---|---|---|
| El JSON no parsea tras la actualización | Respuesta tipo `Funcionalidad[]` mal formateada | Restaurar `agente.json` desde git, pedir al usuario que corrija el bloque |
| Hay sentinels `PENDIENTE_CUESTIONARIO` tras incorporar | Pregunta nueva no estaba en el cuestionario | Volver a ejecutar `/generate-dds` para regenerar el cuestionario |
| El cuestionario tiene preguntas duplicadas | Versiones distintas del cuestionario | Conservar la versión más reciente (con más respuestas) |

## Cuándo NO usar este workflow

- Si no hay un cuestionario en `data/<proyecto>/`: usa `/generate-dds <proyecto>` directamente.
- Si solo quieres re-renderizar tras editar el JSON manualmente: ejecuta `node tools/render-docx.js <proyecto>` directamente.
