/**
 * Resolución centralizada de rutas para los renderizadores DDS.
 *
 * Funciona en dos modos:
 *
 *  1) Modo "repo local": cuando `tools/` y `Plantillas/` están en el mismo
 *     proyecto (estructura clásica del repo DDSAgent). `<root>` es el padre
 *     de `tools/` y todo (Plantillas, data, output, projects) cuelga de ahí.
 *
 *  2) Modo "skill global": cuando `tools/` vive dentro de la skill instalada
 *     en `%APPDATA%\devin\skills\generate-dds-santander\` (o equivalente).
 *     Las plantillas se toman del propio directorio de la skill, mientras
 *     que `data/`, `output/` y `projects/` se resuelven respecto al `cwd`
 *     desde el que el usuario invoca el script.
 *
 * También puede forzarse el modo con la variable de entorno DDS_DATA_ROOT
 * (apunta al directorio que contiene `data/`, `output/`, `projects/`).
 */
const fs = require('fs');
const path = require('path');

const SKILL_HOME = path.resolve(__dirname, '..');
const LOCAL_PLANTILLAS = path.join(SKILL_HOME, 'Plantillas');
const TEMPLATE_ROOT = LOCAL_PLANTILLAS;

/**
 * Resolución de DATA_ROOT (donde están data/, output/, projects/):
 *   1) Si DDS_DATA_ROOT está definido en el entorno → ese.
 *   2) Si <cwd>/data existe → cwd (proyecto del usuario).
 *   3) En otro caso → SKILL_HOME (modo repo DDSAgent clásico).
 */
function detectDataRoot() {
  if (process.env.DDS_DATA_ROOT) return path.resolve(process.env.DDS_DATA_ROOT);
  const cwd = process.cwd();
  if (fs.existsSync(path.join(cwd, 'data'))) return cwd;
  return SKILL_HOME;
}

const DATA_ROOT = detectDataRoot();

function resolveProject(projectArg) {
  if (!projectArg) {
    console.error('Uso: node <script> <proyecto>');
    console.error('Ejemplo: node tools/render-docx.js mi-proyecto');
    console.error('');
    console.error('DATA_ROOT actual:', DATA_ROOT);
    console.error('TEMPLATE_ROOT actual:', TEMPLATE_ROOT);
    process.exit(1);
  }
  // permitir tanto "<proyecto>" como "data/<proyecto>/agente.json"
  const normalized = projectArg
    .replace(/^data[\\/]/, '')
    .replace(/[\\/]agente\.json$/, '')
    .replace(/[\\/]$/, '');

  const dataDir = path.join(DATA_ROOT, 'data', normalized);
  const jsonPath = path.join(dataDir, 'agente.json');
  if (!fs.existsSync(jsonPath)) {
    console.error(`ERROR — No existe ${jsonPath}`);
    console.error(`   Crea data/${normalized}/agente.json o usa /add-new-project ${normalized}`);
    process.exit(2);
  }

  const outputDir = path.join(DATA_ROOT, 'output', normalized);
  fs.mkdirSync(outputDir, { recursive: true });

  const templateMd = path.join(TEMPLATE_ROOT, 'DDS_Plantilla - formato santander.md');
  const templateDocx = path.join(TEMPLATE_ROOT, 'DDS_Plantilla - formato santander.docx');

  if (!fs.existsSync(templateDocx)) {
    console.error(`ERROR — No se encontró la plantilla en ${templateDocx}`);
    console.error('   Configura DDS_DATA_ROOT o coloca las plantillas en Plantillas/.');
    process.exit(6);
  }

  return {
    name: normalized,
    root: DATA_ROOT,
    templateMd,
    templateDocx,
    jsonPath,
    outputDir,
    outputMd: path.join(outputDir, `DDS_${normalized}.md`),
    outputDocx: path.join(outputDir, `DDS_${normalized}.docx`),
  };
}

function loadData(jsonPath) {
  const raw = fs.readFileSync(jsonPath, 'utf8');
  try {
    return JSON.parse(raw);
  } catch (e) {
    console.error(`ERROR — JSON inválido en ${jsonPath}`);
    console.error('  ', e.message);
    process.exit(3);
  }
}

module.exports = { ROOT: DATA_ROOT, SKILL_HOME, TEMPLATE_ROOT, resolveProject, loadData };
