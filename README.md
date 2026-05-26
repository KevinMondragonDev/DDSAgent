# DDSAgent

Generador automatizado del **Documento de Diseño del Sistema (DDS) en formato Santander** a partir de un repositorio de código fuente.

> Entrega un repositorio. Recibe un `.docx` corporativo listo para su incorporación al expediente del proyecto.

## Resumen de uso

```
1. Coloca el código a documentar en   projects/<proyecto>/
2. Pide al agente:                    "/generate-dds <proyecto>"
3. Si hay lagunas: responde el        data/<proyecto>/cuestionario.md
   y luego invoca                     "/answer-questionnaire <proyecto>"
4. Recoge los entregables en          output/<proyecto>/DDS_<proyecto>.docx
```

El agente se encarga del análisis y del renderizado siguiendo la skill `generate-dds-santander`.
**No inventa datos:** si no encuentra evidencia en el código, genera una pregunta concreta
para que el usuario la responda.

---

## Descripcion del repositorio

Este repositorio combina:

- Una **plantilla oficial Santander** (`Plantillas/DDS_Plantilla - formato santander.docx`).
- Una **skill de agente IA** especializada (`generate-dds-santander`) que analiza cualquier repositorio y mapea los hallazgos a las secciones de la plantilla.
- Un **renderizador Java** (`tools/`) que toma un JSON estructurado (`agente.json`) y produce los entregables en `.md` y `.docx` sin perder el formato corporativo.

El JSON intermedio (`data/<proyecto>/agente.json`) es la **única fuente de verdad**.
Se edita el JSON y se vuelve a renderizar; el `.docx` es siempre un artefacto generado.

## Estructura del repositorio

```
DDSAgent/
├── README.md                      <- este archivo
├── AGENTS.md                      <- guía always-on para el agente
├── .windsurf/
│   ├── skills/
│   │   └── generate-dds-santander/
│   │       ├── SKILL.md                  <- procedimiento del agente
│   │       ├── tags-catalog.md           <- contrato placeholders <-> JSON
│   │       ├── analysis-guide.md         <- cómo analizar un repositorio
│   │       ├── rendering-guide.md        <- cómo ejecutar los scripts
│   │       └── questionnaire-format.md   <- formato del cuestionario de lagunas
│   ├── workflows/
│   │   ├── generate-dds.md           <- /generate-dds         (pipeline E2E)
│   │   ├── answer-questionnaire.md   <- /answer-questionnaire (cerrar lagunas)
│   │   └── add-new-project.md        <- /add-new-project      (alta de proyecto)
│   └── rules/
│       └── dds-conventions.md        <- convenciones de redacción (carga automática)
│
├── Plantillas/                    <- plantilla oficial. NO MODIFICAR.
│   ├── DDS_Plantilla - formato santander.docx
│   └── DDS_Plantilla - formato santander.md
│
├── tools/                         <- renderizador Java parametrizado
│   ├── pom.xml                    (descriptor Maven — generado por el agente)
│   ├── README.md                  (referencia de uso del JAR)
│   └── src/main/java/com/santander/dds/
│       ├── Main.java              (punto de entrada)
│       ├── PathsHelper.java       (resolución de rutas)
│       ├── RenderMd.java          (DDS en Markdown)
│       └── RenderDocx.java        (DDS en Word)
│
├── projects/                      <- ENTRADAS: código fuente a documentar
│   ├── README.md                  (convención de alta)
│   └── <proyecto>/                (un subdirectorio por proyecto)
│
├── data/                          <- FUENTE DE VERDAD: JSONs estructurados
│   └── <proyecto>/
│       ├── agente.json            <- JSON estructurado del DDS
│       └── cuestionario.md        <- (solo si hay lagunas) preguntas al usuario
│
└── output/                        <- SALIDAS: entregables generados
    └── <proyecto>/
        ├── DDS_<proyecto>.md
        └── DDS_<proyecto>.docx
```

> Las carpetas `data/`, `output/` y `projects/<proyecto>/` **no están versionadas**; el agente las crea automáticamente al ejecutar `/add-new-project` o `/generate-dds`. El código fuente Java de `tools/` tampoco está versionado: el agente lo genera la primera vez que renderiza, siguiendo `.windsurf/skills/generate-dds-santander/rendering-guide.md`.

## Mecanismos del agente

El repositorio usa tres mecanismos de configuración del agente de IA:

| Mecanismo | Archivo | Activación | Propósito |
|---|---|---|---|
| **Skill** | `.windsurf/skills/generate-dds-santander/SKILL.md` | El modelo decide cuándo invocarla por su `description` | Procedimiento completo + recursos (catálogo, guías) |
| **Workflow** | `.windsurf/workflows/generate-dds.md` | Manual con `/generate-dds <proyecto>` | Runbook reproducible de extremo a extremo |
| **Rule** | `.windsurf/rules/dds-conventions.md` | `model_decision` | Convenciones de redacción (idioma, tono, trazabilidad) |
| **AGENTS.md** | raíz | Always-on | Mapa rápido + reglas irrenunciables |

## Primer uso (5 minutos)

### Requisitos

- **Java** 17 o superior.
- **Maven** instalado.
- Un agente de IA compatible con skills y workflows (p. ej., Windsurf).

### Compilar el renderizador (una sola vez por equipo)

El agente crea el código fuente Java automáticamente si no existe. Si quieres compilar manualmente después de que el agente lo haya creado:

```powershell
mvn -f tools/pom.xml clean package -q
```

## Documentar un proyecto nuevo

### Opcion A — con slash command (recomendado)

```
/add-new-project <mi-proyecto>
```

Coloca el código fuente en `projects/<mi-proyecto>/` y lanza:

```
/generate-dds <mi-proyecto>
```

El agente analiza el repositorio y produce `data/<mi-proyecto>/agente.json`. Pueden ocurrir dos situaciones:

1. **Sin lagunas** — renderiza directamente los entregables en `output/<mi-proyecto>/`.
2. **Con lagunas** — genera `data/<mi-proyecto>/cuestionario.md` con preguntas concretas y se detiene. El usuario responde el archivo y ejecuta:
   ```
   /answer-questionnaire <mi-proyecto>
   ```
   El agente incorpora las respuestas al JSON y renderiza los entregables.

### Opcion B — manual

```powershell
# 1. Crear carpetas
New-Item -ItemType Directory -Force -Path "projects/<mi-proyecto>", "data/<mi-proyecto>", "output/<mi-proyecto>"

# 2. Copiar el código fuente a projects/<mi-proyecto>/

# 3. Pedir al agente:
#    "Genera el DDS Santander del proyecto <mi-proyecto> siguiendo la skill generate-dds-santander"
```

## Iterar sobre un DDS ya generado

No editar el `.docx` directamente; es un artefacto generado.

```powershell
# 1. Editar data/<mi-proyecto>/agente.json
# 2. Volver a renderizar:
java -jar tools/target/dds-tools.jar <mi-proyecto>
```

## Guia de evolución del repositorio

| Para mejorar... | Editar | Notas |
|---|---|---|
| El procedimiento del agente (cómo analiza) | `.windsurf/skills/generate-dds-santander/SKILL.md` y `analysis-guide.md` | El modelo los carga progresivamente. |
| El catálogo de placeholders (nuevos campos) | `.windsurf/skills/generate-dds-santander/tags-catalog.md` + `tools/` | Mantener ambos sincronizados. |
| El formato del cuestionario | `.windsurf/skills/generate-dds-santander/questionnaire-format.md` | Define cómo el agente reporta lagunas. |
| Las convenciones de redacción | `.windsurf/rules/dds-conventions.md` | Trigger `model_decision`. |
| El runbook de generación | `.windsurf/workflows/generate-dds.md` | Invocable con `/generate-dds`. |
| El runbook de cierre de lagunas | `.windsurf/workflows/answer-questionnaire.md` | Invocable con `/answer-questionnaire`. |
| El renderizado general | `tools/src/main/java/.../RenderMd.java` | Genera el Markdown de salida. |
| El renderizado del Word | `tools/src/main/java/.../RenderDocx.java` | Lógica de reemplazo OOXML. |
| La estructura del JSON | `tools/src/main/java/.../PathsHelper.java` | Cambios estructurales requieren migrar JSONs existentes. |
| La plantilla Santander | **No editar.** Reemplazar en `Plantillas/` si hay nueva versión oficial. | Seguir el procedimiento en `dds-conventions.md`. |

## Archivos que no deben modificarse

- `Plantillas/*` — plantilla oficial. Solo se reemplaza si Santander publica una nueva versión.
- `output/*` — se regenera automáticamente al renderizar.

## Glosario

| Término | Definición |
|---|---|
| **DDS** | Documento de Diseño del Sistema. Documento corporativo Santander que describe arquitectura, funcionalidades, requisitos y plan de un sistema software. |
| **Placeholder** | Marcador `{{nombre}}` en la plantilla que el renderizador sustituye por contenido del JSON. |
| **Sentinel `PENDIENTE_CUESTIONARIO`** | Valor especial asignado en `agente.json` cuando no se encuentra evidencia en el código. Marca el campo como pendiente de respuesta del usuario. |
| **Cuestionario** | Archivo `data/<proyecto>/cuestionario.md` generado por el agente cuando hay lagunas, con una pregunta concreta por cada sentinel. |
| **Skill** | Mecanismo de configuración del agente que empaqueta un procedimiento y archivos de soporte. |
| **Workflow** | Runbook ejecutable manualmente mediante un slash command. |
| **Rule** | Restricción de comportamiento del agente que se carga automáticamente en situaciones relevantes. |
