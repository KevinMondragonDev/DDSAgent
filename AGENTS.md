# AGENTS.md — DDSAgent

Repositorio especializado en **generar el Documento de Diseño del Sistema (DDS) en formato Santander** a partir del análisis automatizado de proyectos de software.

## Rol del agente en este repositorio

El agente tiene tres responsabilidades principales:

1. **Analizar** el proyecto colocado en `projects/<nombre>/`.
2. **Producir** un `agente.json` en `data/<nombre>/` con todos los placeholders de la plantilla Santander rellenos con información verificable en el código.
3. **Renderizar** el JSON como `.md` y `.docx` en `output/<nombre>/`.

La maquinaria oficial está empaquetada como una skill: **`generate-dds-santander`** (en `.windsurf/skills/`). Invocarla cuando el usuario solicite un DDS.

## Mapa rápido del repositorio

| Carpeta | Propósito | Editable por el agente |
|---|---|---|
| `Plantillas/` | Plantilla oficial Santander (`.docx` + `.md`) | **No. Inmutable.** |
| `projects/<nombre>/` | Código fuente del proyecto a documentar | Solo el usuario. |
| `data/<nombre>/agente.json` | Fuente de verdad estructurada del DDS | Sí. |
| `data/<nombre>/cuestionario.md` | Preguntas al usuario para cerrar lagunas | Sí. |
| `output/<nombre>/` | Entregables finales (`.md`, `.docx`) | No. Se regenera automáticamente. |
| `tools/` | Scripts de renderizado Node.js | Sí, con cuidado. |
| `.windsurf/skills/` | Skill `generate-dds-santander` y sus recursos | Sí, evolutivo. |
| `.windsurf/workflows/` | Slash commands `/generate-dds`, `/add-new-project` | Sí, evolutivo. |
| `.windsurf/rules/` | Convenciones de redacción Santander | Sí, evolutivo. |

## Reglas irrenunciables

1. **Cero invención.** Si un dato no tiene evidencia en `projects/<nombre>/`:
   - En `agente.json`: asignar el sentinel literal `"PENDIENTE_CUESTIONARIO"`.
   - Generar o actualizar `data/<nombre>/cuestionario.md` con una pregunta concreta, siguiendo `.windsurf/skills/generate-dds-santander/questionnaire-format.md`.
   - Añadir el resumen a `pendientes[]`.
   - Nunca rellenar con texto plausible para "tapar" la laguna.
2. **El JSON es la única fuente de verdad.** No editar el `.docx` a mano; se regenera a partir del JSON.
3. **No modificar `Plantillas/`** salvo que Santander publique una nueva versión oficial.
4. **Anexo 1 obligatorio.** Los 7 requisitos SO/FI/DI/US deben evaluarse y justificarse para cada proyecto.
5. **Idioma único.** Español neutro técnico formal. Sin mezclar idiomas, sin emojis, sin lenguaje de marketing.
6. **No entregar con sentinels.** Un `.docx` con `PENDIENTE_CUESTIONARIO` visibles no es un entregable. El cuestionario debe cerrarse primero.

## Cuándo NO invocar la skill

- El usuario solicita "un README" o documentación técnica informal → no es un DDS Santander.
- El usuario solicita explicar el código a otra persona → es una explicación, no un DDS.
- El usuario solicita corregir un error en el código del proyecto analizado → intervenir en el repositorio del proyecto, no generar documentación.

## Para mayor detalle

- **Onboarding:** `README.md` (raíz).
- **Cómo analizar un repositorio:** `.windsurf/skills/generate-dds-santander/analysis-guide.md`.
- **Qué claves rellenar:** `.windsurf/skills/generate-dds-santander/tags-catalog.md`.
- **Cómo manejar lagunas:** `.windsurf/skills/generate-dds-santander/questionnaire-format.md`.
- **Cómo renderizar:** `.windsurf/skills/generate-dds-santander/rendering-guide.md` y `tools/README.md`.
- **Convenciones de redacción:** `.windsurf/rules/dds-conventions.md`.
- **Slash commands:** `/generate-dds`, `/answer-questionnaire`, `/add-new-project` (ver `.windsurf/workflows/`).
