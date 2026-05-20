# Formato canónico de `cuestionario.md`

Cuando durante la **Fase 2 (producción de `agente.json`)** el agente detecta una laguna —un campo del DDS para el que no hay evidencia en `projects/<proyecto>/`— **NO debe inventar ni dejar el campo vacío**. Debe:

1. Marcar el campo con `"PENDIENTE_CUESTIONARIO"` en `agente.json` (es un sentinel detectable).
2. Añadir una pregunta correspondiente en `data/<proyecto>/cuestionario.md`.
3. Listar la entrada también en `pendientes[]` del JSON.

El usuario responde en el `.md`. Después, con `/answer-questionnaire <proyecto>`, el agente reabsorbe las respuestas en el JSON y re-renderiza.

## Estructura del archivo

```markdown
# Cuestionario — DDS de <proyecto>

> Generado automáticamente por la skill `generate-dds-santander`.
> Responde cada bloque ` ``` ` con la información solicitada y luego pídele al agente
> `/answer-questionnaire <proyecto>` para actualizar el DDS.

## Estado

- **Proyecto:** `<proyecto>`
- **Generado:** DD/MM/YYYY
- **Preguntas pendientes:** N
- **Preguntas respondidas:** 0/N

## Cómo responder

1. Escribe tu respuesta dentro del bloque ` ``` ` que aparece debajo de cada pregunta.
2. **No** elimines la pregunta ni cambies su `key`; el agente la usa para localizar el campo en `agente.json`.
3. Cuando termines (parcial o totalmente), invoca `/answer-questionnaire <proyecto>`.
4. Las respuestas vacías se ignoran (quedan como `PENDIENTE_CUESTIONARIO` hasta el próximo ciclo).

---

## §<sección> — <nombre de sección DDS>

### Q-<NN>. <Pregunta clara y concreta>

- **Clave JSON:** `<ruta.al.campo.en.agente.json>`
- **Tipo esperado:** `string` | `string[]` | `{...}` | `Funcionalidad[]`
- **Por qué pregunto:** <breve explicación de qué se buscó y no se encontró>
- **Pista:** <dónde podría buscar el usuario o qué formato dar>
- **Bloqueante:** Sí | No

**Respuesta:**
```text
<el usuario llena este bloque>
```

---
```

## Reglas duras del formato

1. **El bloque de respuesta DEBE ser un fenced code block con lenguaje `text`** para no confundirlo con código real.
2. **El identificador `Q-<NN>` es estable**: si la misma pregunta vuelve a generarse en una segunda pasada, conserva el `Q-<NN>` original.
3. **`Clave JSON` debe ser una ruta JSONPath simplificada** (puntos para anidamiento, `[i]` para arrays, `[*]` para "todos los elementos del array"). Ejemplos:
   - `metadata.codigo_proyecto`
   - `funcionalidades_sistema[2].descripcion`
   - `anexo_requisitos[*].justificacion`
4. **Las preguntas se agrupan por sección DDS** usando `## §<sección>`. Mantén el orden del JSON.
5. **`Bloqueante: Sí`** marca preguntas sin las cuales el DDS no se puede entregar (p. ej. nombre del proyecto). `No` marca preguntas que solo enriquecen.

## Tipología de preguntas comunes

| Sección JSON | Cuándo preguntar | Ejemplo |
|---|---|---|
| `metadata.codigo_proyecto` | El repo no contiene un código `MX-…`. | "¿Cuál es el código de proyecto Santander?" |
| `proposito` | El README del repo no explicita el propósito de negocio. | "¿Cuál es el propósito de negocio que justifica este sistema?" |
| `objetivos_beneficios[]` | Hay propósito pero no objetivos cuantificables. | "Lista 3-5 objetivos concretos del sistema (KPIs medibles si aplica)." |
| `principales_conceptos_relaciones.entidades[].descripcion` | El tipo existe en código pero no hay docstring. | "Describe en una frase la entidad `Receta`." |
| `definicion_roles[].rol` | No hay autenticación detectable. | "¿Qué actores/roles utilizan el sistema?" |
| `anexo_requisitos[i].justificacion` | El requisito SO/FI/DI/US no se puede deducir del código. | "¿La aplicación opera 24h? Justifica la disponibilidad esperada." |
| `resguardo_documentos_por_tipo_documental` | El sistema no maneja documentos formales. | "¿Qué tipos documentales se generan y cuál es su política de retención?" |
| `plan_iteraciones[*]` | No hay roadmap visible. | "Describe las próximas 2-3 iteraciones planeadas." |

## Sentinel value

En `agente.json`, los campos sin respuesta del usuario quedan exactamente como:

```json
"campo": "PENDIENTE_CUESTIONARIO"
```

Esto permite a los renderizadores y validadores detectar de manera trivial los campos que aún no deben publicarse. Los scripts `tools/render-*.js` los renderizan como literal `"PENDIENTE_CUESTIONARIO"` (queda visible) — **NO debe entregarse un DDS con sentinels presentes.**

## Ciclo de vida del cuestionario

```
[1] Análisis genera cuestionario.md      ← Fase 2 de generate-dds
        ↓
[2] Usuario edita las respuestas         ← Trabajo manual, sin restricciones
        ↓
[3] /answer-questionnaire <proyecto>     ← Agente reabsorbe
        ↓
[4] agente.json actualizado              ← Sentinel reemplazado por respuesta
        ↓
[5] Re-renderizado (md + docx)           ← /generate-dds o tools/render-*
        ↓
        ¿Aún quedan PENDIENTE_CUESTIONARIO?
            Sí → vuelve a [1] (cuestionario actualizado, solo gaps restantes)
            No → entregar
```
