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

Esta tabla cubre **todos los campos** del esqueleto JSON que pueden quedar como `PENDIENTE_CUESTIONARIO`. Generar la pregunta correspondiente en cuanto no haya evidencia en el código.

### Metadatos

| Sección JSON | Cuándo preguntar | Tipo esperado | Bloqueante |
|---|---|---|---|
| `metadata.codigo_proyecto` | El repo no contiene un código `MX-…` en README, manifests ni comentarios. | `string` (`MX-XXX-NNN`) | Sí |
| `metadata.descripcion_de_los_cambios` | Primera versión del documento (no hay historial). | `string` | No |

### Sección 1 — Introducción

| Sección JSON | Cuándo preguntar | Tipo esperado | Bloqueante |
|---|---|---|---|
| `introduccion` | El README no tiene párrafo de contexto corporativo del sistema. | `string` multilínea | Sí |
| `proposito` | El README no explicita el propósito de negocio (solo describe la tecnología). | `string` | Sí |
| `alcance` | No hay documentación de qué incluye y qué excluye el sistema. | `string` | Sí |
| `objetivos_beneficios[]` | Hay propósito pero no objetivos cuantificables ni KPIs. | `string[]` (uno por línea) | No |
| `descripcion_del_sistema` | La arquitectura general no se puede inferir solo del código. | `string` multilínea | Sí |
| `vocabulario[]` | El dominio tiene términos o acrónimos que no se explican en el código. | `{termino, definicion}[]` como JSON | No |

### Sección 2 — Situación actual

| Sección JSON | Cuándo preguntar | Tipo esperado | Bloqueante |
|---|---|---|---|
| `situacion_actual` | No hay Dockerfile, pipeline CI/CD, ni README de infraestructura. | `string` multilínea | Sí |

### Sección 3 — Análisis y requisitos

| Sección JSON | Cuándo preguntar | Tipo esperado | Bloqueante |
|---|---|---|---|
| `analisis_y_definicion_de_requisitos` | No hay documentáción de metodología de análisis. | `string` | No |
| `principales_conceptos_relaciones.entidades[].descripcion` | El tipo/clase existe en código pero sin docstring ni comentario. | `string` por entidad | No |
| `principales_conceptos_relaciones.relaciones[]` | No hay ORM, claves foráneas ni referencias entre tipos detectables. | `string[]` | No |
| `funcionalidades_sistema[].nombre` | El nombre de la funcionalidad es técnico (nombre de componente), no de negocio. | `string` | Sí |
| `funcionalidades_sistema[].descripcion` | La descripción no es verificable en el código. | `string` | Sí |
| `definicion_roles[].rol` | No hay autenticación, guards ni middleware de permisos detectables. | `{rol, responsabilidad}[]` como JSON | Sí |
| `division_sistema.capas[]` | La arquitectura por capas no es evidente en la estructura de carpetas. | `{nombre, descripcion}[]` como JSON | No |
| `caso_uso_principal.nombre` | No hay README de flujos ni documentación de casos de uso. | `string` | Sí |
| `caso_uso_principal.actor` | No se puede inferir el actor principal del sistema. | `string` | Sí |
| `caso_uso_principal.precondicion` | No hay validaciones de estado previo detectables en el código. | `string` | No |
| `caso_uso_principal.flujo_basico[]` | El flujo completo no se puede reconstruir solo del código. | `string[]` (pasos numerados) | Sí |
| `caso_uso_principal.postcondicion` | No hay documentación del estado final esperado. | `string` | No |

### Sección 4 — Tabla de requisitos

| Sección JSON | Cuándo preguntar | Tipo esperado | Bloqueante |
|---|---|---|---|
| `tabla_requisitos[]` | Los requisitos inferidos del código son insuficientes (menos de 5). | `{id, tipo, requisito, prioridad}[]` como JSON | Sí |

### Sección 5 — XBRL

| Sección JSON | Cuándo preguntar | Tipo esperado | Bloqueante |
|---|---|---|---|
| `informes_xbrl` | El sistema procesa datos financieros o reportes regulatorios y no hay claridad sobre XBRL. | `string` | No |

### Sección 6 — Documentación

| Sección JSON | Cuándo preguntar | Tipo esperado | Bloqueante |
|---|---|---|---|
| `documentacion_a_producir[]` | No hay plan de documentación más allá del propio DDS. | `string[]` (uno por línea) | No |

### Sección 7 — Plan de iteraciones

| Sección JSON | Cuándo preguntar | Tipo esperado | Bloqueante |
|---|---|---|---|
| `plan_iteraciones[*]` | No hay roadmap, milestones ni backlog visible en el repositorio. | `{iteracion, nombre, alcance, estado}[]` como JSON | No |

### Sección 8 — Resguardo documental

| Sección JSON | Cuándo preguntar | Tipo esperado | Bloqueante |
|---|---|---|---|
| `resguardo_documentos_por_tipo_documental` | Siempre preguntar si el sistema no gestiona explícitamente tipos documentales formales. | `string` | No |

### Anexo 1 — Requisitos estructurales Santander

| Sección JSON | Cuándo preguntar | Tipo esperado | Bloqueante |
|---|---|---|---|
| `anexo_requisitos[SO001].justificacion` | No hay concepto de organización, workspace o tenant en el código. | `string` | Sí |
| `anexo_requisitos[SO002].justificacion` | El sistema gestiona valores monetarios pero no hay evidencia de multidivisa. | `string` | Sí |
| `anexo_requisitos[SO003].justificacion` | La lógica de negocio y la presentación están mezcladas en el código. | `string` | Sí |
| `anexo_requisitos[SO004].justificacion` | No hay archivos de internacionalización (i18n) detectables. | `string` | Sí |
| `anexo_requisitos[FI001].justificacion` | No hay SLA ni arquitectura stateless/replicada documentada. | `string` | Sí |
| `anexo_requisitos[DI001].justificacion` | No hay integraciones con sistemas corporativos del banco detectables. | `string` | Sí |
| `anexo_requisitos[US001].justificacion` | No hay configuración de compatibilidad de navegadores ni tests de compatibilidad. | `string` | Sí |

## Sentinel value

En `agente.json`, los campos sin respuesta del usuario quedan exactamente como:

```json
"campo": "PENDIENTE_CUESTIONARIO"
```

Esto permite a los renderizadores y validadores detectar de manera trivial los campos que aún no deben publicarse. El JAR `tools/target/dds-tools.jar` los renderiza como literal `"PENDIENTE_CUESTIONARIO"` (queda visible) — **NO debe entregarse un DDS con sentinels presentes.**

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
[5] Re-renderizado (md + docx)           ← /generate-dds o java -jar tools/target/dds-tools.jar
        ↓
        ¿Aún quedan PENDIENTE_CUESTIONARIO?
            Sí → vuelve a [1] (cuestionario actualizado, solo gaps restantes)
            No → entregar
```
