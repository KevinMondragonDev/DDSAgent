# `tools/` — Renderizadores del DDS Santander

Scripts Node.js que toman `data/<proyecto>/agente.json` y producen los entregables en
`output/<proyecto>/`. No requieren TypeScript ni paso de compilación.

## Requisitos

- **Node.js** >= 18 (verificado con 20 y 24).
- **npm** >= 9.

## Instalacion (una sola vez por equipo)

```powershell
cd tools
npm install
```

Instala una única dependencia: `pizzip` (~50 KB), parser ZIP en JavaScript puro, usado
para manipular el OOXML del `.docx`.

## Comandos

| Comando | Salida |
|---|---|
| `node tools/render-md.js <proyecto>` | `output/<proyecto>/DDS_<proyecto>.md` |
| `node tools/render-docx.js <proyecto>` | `output/<proyecto>/DDS_<proyecto>.docx` |

Donde `<proyecto>` es el nombre del subdirectorio en `data/`.

Ejemplo con un proyecto hipotético llamado `mi-sistema`:

```powershell
node tools/render-md.js   mi-sistema
node tools/render-docx.js mi-sistema
```

El script también acepta rutas más explícitas (se normalizan internamente):

```powershell
node tools/render-docx.js data/mi-sistema
node tools/render-docx.js data/mi-sistema/agente.json
```

## Codigos de salida

| Código | Significado |
|---|---|
| `0` | Éxito. Archivo generado correctamente. |
| `1` | Falta el argumento `<proyecto>`. |
| `2` | No existe `data/<proyecto>/agente.json`. |
| `3` | JSON inválido (error de parseo). |
| `4` | La plantilla `.docx` no contiene filas de vocabulario. La plantilla puede haber sido modificada. |
| `5` | Quedaron placeholders `{{…}}` sin sustituir. El archivo de salida **no se escribe**. |

## Arquitectura interna

- **`paths.js`** — Resolución centralizada de rutas (`<root>`, plantilla, JSON, salida) y
  carga/validación del JSON.
- **`render-md.js`** — Sustituye todos los placeholders en el `.md` y escribe el archivo
  de salida. Incluye normalización de escapes de Google Docs (`\_`, `*` dentro de `{{...}}`).
- **`render-docx.js`** — Abre el `.docx` con PizZip, normaliza placeholders en el XML OOXML,
  expande la tabla de vocabulario, expande la tabla de requisitos y sustituye el resto de
  placeholders. Valida que no queden `{{…}}` residuales antes de escribir.

La convención completa de placeholders se documenta en
`.windsurf/skills/generate-dds-santander/tags-catalog.md`.

## Normalizacion de escapes de Google Docs

La plantilla se exporta desde Google Docs, lo que introduce:

- `\_` como escape Markdown para guiones bajos en nombres de campo.
- `*` como delimitador de cursiva alrededor de palabras dentro de placeholders.

Ambos scripts normalizan estos escapes **antes** de buscar placeholders, de modo que
`{{fecha\_actual\_ddmmaa}}` y `{{Principales_*conceptos_y*_relaciones_entre_ellos}}`
se resuelven correctamente sin modificar la plantilla fuente.

## Loops de tabla

### Vocabulario

La plantilla tiene 6 filas plantilla con `{{#vocabulario}}` y `{{termino}}`. El
renderizador las expande a N filas, una por entrada de `vocabulario[]` en el JSON.

### Requisitos

La plantilla tiene 5 filas plantilla con `{{id_req}}`, `{{tipo_req}}`, `{{descripcion_req}}`
y `{{prioridad_req}}`. El renderizador las expande a N filas, una por entrada de
`tabla_requisitos[]` en el JSON.

## Cuando modificar estos scripts

- **Nueva version de la plantilla Santander** — Re-inspeccionar `document.xml` y ajustar
  `render-docx.js`. Actualizar también `tags-catalog.md`.
- **Nuevo placeholder en la plantilla** — Añadirlo al array `replacements` en `render-docx.js`
  y al objeto `simple` en `render-md.js`. Actualizar `tags-catalog.md`.
- **Cambio en la forma de un campo del JSON** — Ajustar el renderizador correspondiente
  y `tags-catalog.md`.

## Validacion rapida del JSON (sin renderizar)

```powershell
node -e "JSON.parse(require('fs').readFileSync('data/<proyecto>/agente.json','utf8'));console.log('OK')"
```

## Limitaciones conocidas

- Los saltos de línea dentro de placeholders multilínea se renderizan como `<w:br/>` en el
  `.docx` (mismo párrafo). Si se requieren párrafos separados en OOXML, es necesario
  insertar `</w:p><w:p>` — mejora pendiente.
- No se generan diagramas UML. Las secciones de diagrama quedan en texto; los diagramas
  deben adjuntarse manualmente al `.docx` si se requieren.
