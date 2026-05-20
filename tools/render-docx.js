#!/usr/bin/env node
/**
 * Renderiza el DDS Santander en Word (.docx).
 *
 * Uso:
 *   node tools/render-docx.js <proyecto>
 *
 * Lee:
 *   Plantillas/DDS_Plantilla - formato santander.docx
 *   data/<proyecto>/agente.json
 *
 * Escribe:
 *   output/<proyecto>/DDS_<proyecto>.docx
 *
 * Estrategia:
 *  - El .docx es un ZIP de OOXML (Open XML).
 *  - La sustitución se hace a nivel de párrafo: dentro de cada <w:p> se concatena el texto
 *    de todos sus <w:t>, se buscan placeholders {{...}} en el texto lógico, y se
 *    redistribuyen los valores (primer <w:t> recibe el valor, los demás afectados se vacían).
 *    Esto permite manejar placeholders fragmentados en múltiples runs (típico en exportes
 *    de Google Docs donde el formato cursivo parte el placeholder).
 *  - `buildPlaceholderAliases` registra automáticamente la variante sin "_" de cada key,
 *    para casos en que el DOCX descarta separadores entre fragmentos estilizados.
 *  - La tabla de vocabulario tiene filas plantilla con {{#vocabulario}} / {{termino}};
 *    se expanden a N filas (una por entrada del JSON).
 *  - La tabla de requisitos tiene filas plantilla con {{id_req}} / {{tipo_req}} / etc.;
 *    se expanden a N filas (una por requisito).
 *  - Para texto multilínea se convierten saltos de línea en <w:br/> dentro del mismo párrafo.
 */
const fs = require('fs');
const PizZip = require('pizzip');
const { resolveProject, loadData } = require('./paths');

const ctx = resolveProject(process.argv[2]);
const data = loadData(ctx.jsonPath);

// ---------- helpers ----------
const escapeXml = (s) =>
  String(s)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;');

/** Convierte texto multilínea en contenido w:t con saltos OOXML. */
const multilineToWt = (s) => {
  const lines = String(s).split(/\r?\n/);
  return lines.map(escapeXml).join('</w:t><w:br/><w:t xml:space="preserve">');
};

const renderBullets = (arr) => arr.map((x) => `• ${x}`).join('\n');

const renderFuncionalidades = (items) =>
  items
    .map(
      (f) =>
        `${f.id} — ${f.nombre}\n` +
        `  ${f.descripcion}\n` +
        `  Componentes: ${f.componentes.join(', ')}\n` +
        `  Fuente: ${f.fuente}`,
    )
    .join('\n\n');

const renderConceptos = (pcr) => {
  const entidades = pcr.entidades
    .map((e) => `${e.nombre}: ${e.descripcion}`)
    .join('\n');
  const relaciones =
    pcr.relaciones && pcr.relaciones.length
      ? '\n\nRelaciones:\n' + pcr.relaciones.map((r) => `• ${r}`).join('\n')
      : '';
  return entidades + relaciones;
};

const renderRoles = (items) =>
  items.map((r) => `${r.rol}: ${r.responsabilidad}`).join('\n');

const renderCapas = (capas) =>
  capas.map((c) => `${c.nombre}: ${c.descripcion}`).join('\n');

const renderCasoUso = (cu) =>
  `Caso de uso: ${cu.nombre}\n` +
  `Actor: ${cu.actor}\n` +
  `Precondición: ${cu.precondicion}\n\n` +
  `Flujo básico:\n` +
  cu.flujo_basico.map((p, i) => `${i + 1}. ${p}`).join('\n') +
  `\n\nPostcondición: ${cu.postcondicion}`;

const renderPlan = (items) =>
  items.map((i) => `${i.iteracion}. ${i.nombre} — ${i.alcance} [${i.estado}]`).join('\n');

// ---------- sustitución a nivel de párrafo ----------
/**
 * Los placeholders en el OOXML pueden estar fragmentados en múltiples runs por Word
 * (por ej. cursivas en medio de "{{codigo_del_proyecto}}" o "{{introduccion}}").
 *
 * Esta función procesa el XML párrafo por párrafo:
 *  1. Recolecta todos los <w:t>...</w:t> dentro de cada <w:p>...</w:p>.
 *  2. Concatena su texto para obtener el "texto lógico" del párrafo.
 *  3. Busca placeholders {{...}} en el texto lógico y los sustituye por sus valores.
 *  4. Redistribuye el resultado de vuelta a los <w:t>:
 *     - El primer <w:t> recibe el prefijo previo al placeholder + el valor sustituido.
 *     - Los <w:t> intermedios se vacían.
 *     - El último <w:t> conserva el sufijo posterior al placeholder.
 *
 * Notas:
 *  - Los valores en `subs` pueden contener fragmentos XML válidos (p.ej. </w:t><w:br/><w:t...>
 *    para texto multilínea); se inyectan dentro del <w:t> y la propia inyección cierra/abre
 *    nuevos <w:t> correctamente.
 *  - Se normalizan escapes de Google Docs (\_, \#, *) y se reconstruyen tanto la forma
 *    "limpia" como la concatenada-sin-separadores de cada key.
 */
function buildPlaceholderAliases(subs) {
  // Para cada key con guiones bajos, registramos también la variante sin "_" (que es lo
  // que produce el DOCX cuando un fragmento estilizado descarta los separadores).
  const out = Object.assign({}, subs);
  for (const [k, v] of Object.entries(subs)) {
    const stripped = k.replace(/_/g, '');
    if (stripped !== k && !(stripped in out)) out[stripped] = v;
  }
  return out;
}

function applySubstitutions(xml, subs) {
  const aliases = buildPlaceholderAliases(subs);
  return xml.replace(/(<w:p\b[^>]*>)([\s\S]*?)(<\/w:p>)/g, (full, pOpen, pBody, pClose) => {
    const wts = [];
    const wtRe = /(<w:t[^>]*>)([\s\S]*?)(<\/w:t>)/g;
    let m;
    while ((m = wtRe.exec(pBody)) !== null) {
      wts.push({
        open: m[1],
        text: m[2],
        close: m[3],
        start: m.index,
        end: m.index + m[0].length,
      });
    }
    if (!wts.length) return full;

    const fullText = wts.map((w) => w.text).join('');
    if (!fullText.includes('{{')) return full;

    // Mapa posición → índice de <w:t>.
    const posToWt = [];
    wts.forEach((wt, i) => {
      for (let j = 0; j < wt.text.length; j++) posToWt.push(i);
    });

    // Buscar todos los placeholders en el texto concatenado del párrafo.
    const phRe = /\{\{[^}]*?\}\}/g;
    let phM;
    const changes = [];
    while ((phM = phRe.exec(fullText)) !== null) {
      const ph = phM[0];
      if (ph in aliases) {
        changes.push({
          phStart: phM.index,
          phEnd: phM.index + ph.length,
          ph,
          replacement: aliases[ph],
        });
      }
    }
    if (!changes.length) return full;

    // Aplicar de derecha a izquierda para preservar índices.
    changes.sort((a, b) => b.phStart - a.phStart);
    const newTexts = wts.map((w) => w.text);

    for (const { phStart, phEnd, ph, replacement } of changes) {
      const firstWt = posToWt[phStart];
      const lastWt = posToWt[phEnd - 1];

      if (firstWt === lastWt) {
        newTexts[firstWt] = newTexts[firstWt].split(ph).join(replacement);
      } else {
        let startOff = phStart;
        for (let i = 0; i < firstWt; i++) startOff -= wts[i].text.length;
        let endOff = phEnd;
        for (let i = 0; i < lastWt; i++) endOff -= wts[i].text.length;

        newTexts[firstWt] = newTexts[firstWt].slice(0, startOff) + replacement;
        for (let i = firstWt + 1; i < lastWt; i++) newTexts[i] = '';
        newTexts[lastWt] = newTexts[lastWt].slice(endOff);
      }
    }

    // Reconstruir pBody (de derecha a izquierda para no invalidar offsets).
    let newBody = pBody;
    for (let i = wts.length - 1; i >= 0; i--) {
      if (newTexts[i] === wts[i].text) continue;
      const wt = wts[i];
      newBody = newBody.slice(0, wt.start) + wt.open + newTexts[i] + wt.close + newBody.slice(wt.end);
    }
    return pOpen + newBody + pClose;
  });
}

// ---------- abrir docx ----------
const buf = fs.readFileSync(ctx.templateDocx);
const zip = new PizZip(buf);

let docXml = zip.file('word/document.xml').asText();

// ---------- 1. Tabla de vocabulario (loop) ----------
const trRegex = /<w:tr\b[^>]*>[\s\S]*?<\/w:tr>/g;
const vocabRows = [];
let m;
while ((m = trRegex.exec(docXml)) !== null) {
  if (m[0].includes('{{#vocabulario}}') && m[0].includes('{{termino}}')) {
    vocabRows.push({ start: m.index, end: m.index + m[0].length, xml: m[0] });
  }
}
if (vocabRows.length === 0) {
  console.error('ERROR — No se encontraron filas de vocabulario en document.xml.');
  process.exit(4);
}
const vocabTemplateRow = vocabRows[0].xml;
const newVocabRows = data.vocabulario
  .map((v) =>
    vocabTemplateRow
      .replace(/\{\{#vocabulario\}\}/g, escapeXml(v.termino))
      .replace(/\{\{termino\}\}/g, escapeXml(v.definicion)),
  )
  .join('');
docXml =
  docXml.slice(0, vocabRows[0].start) +
  newVocabRows +
  docXml.slice(vocabRows[vocabRows.length - 1].end);

// ---------- 2. Tabla de requisitos (loop) ----------
const trRegex2 = /<w:tr\b[^>]*>[\s\S]*?<\/w:tr>/g;
const reqRows = [];
let m2;
while ((m2 = trRegex2.exec(docXml)) !== null) {
  if (
    m2[0].includes('{{id_req}}') &&
    m2[0].includes('{{tipo_req}}') &&
    m2[0].includes('{{descripcion_req}}') &&
    m2[0].includes('{{prioridad_req}}')
  ) {
    reqRows.push({ start: m2.index, end: m2.index + m2[0].length, xml: m2[0] });
  }
}
if (reqRows.length > 0) {
  const reqTemplateRow = reqRows[0].xml;
  const newReqRows = data.tabla_requisitos
    .map((r) =>
      reqTemplateRow
        .replace(/\{\{id_req\}\}/g, escapeXml(r.id))
        .replace(/\{\{tipo_req\}\}/g, escapeXml(r.tipo))
        .replace(/\{\{descripcion_req\}\}/g, escapeXml(r.requisito))
        .replace(/\{\{prioridad_req\}\}/g, escapeXml(r.prioridad)),
    )
    .join('');
  docXml =
    docXml.slice(0, reqRows[0].start) +
    newReqRows +
    docXml.slice(reqRows[reqRows.length - 1].end);
} else {
  console.warn('AVISO — No se encontraron filas de requisitos en document.xml. La tabla puede ser estática.');
}

// ---------- 3. Placeholders simples (sustitución a nivel de párrafo) ----------
const subs = {
  '{{fecha_actual_ddmmaa}}':                 escapeXml(data.metadata.fecha_actual_ddmmaa),
  '{{descripcion_de_los_cambios}}':          escapeXml(data.metadata.descripcion_de_los_cambios),
  '{{codigo_del_proyecto}}':                 escapeXml(data.metadata.codigo_proyecto),
  '{{autor}}':                               escapeXml(data.metadata.autor),
  '{{introduccion}}':                        multilineToWt(data.introduccion),
  '{{proposito}}':                           multilineToWt(data.proposito),
  '{{alcance}}':                             multilineToWt(data.alcance),
  '{{objetivos/beneficios}}':                multilineToWt(renderBullets(data.objetivos_beneficios)),
  // Fallback: el .docx exportado de Google Docs conserva el typo "objectivos" (sin 'j').
  '{{objectivos/beneficios}}':               multilineToWt(renderBullets(data.objetivos_beneficios)),
  '{{descripcion}}':                         multilineToWt(data.descripcion_del_sistema),
  '{{situacion_actual}}':                    multilineToWt(data.situacion_actual),
  '{{analisis_y_definicion_de_requisitos}}': multilineToWt(data.analisis_y_definicion_de_requisitos),
  '{{Principales_conceptos_y_relaciones_entre_ellos}}': multilineToWt(renderConceptos(data.principales_conceptos_relaciones)),
  '{{funcionalidades_sistema}}':             multilineToWt(renderFuncionalidades(data.funcionalidades_sistema)),
  '{{definicion_de_roles}}':                 multilineToWt(renderRoles(data.definicion_roles)),
  '{{division_del_sistema}}':                multilineToWt(renderCapas(data.division_sistema.capas)),
  '{{caso_de_uso_principal}}':               multilineToWt(renderCasoUso(data.caso_uso_principal)),
  '{{Informe_bajo_el_estándar_Xbrl}}':       multilineToWt(data.informes_xbrl),
  '{{documentacion_a_producir}}':            multilineToWt(renderBullets(data.documentacion_a_producir)),
  '{{plan_de_iteraciones}}':                 multilineToWt(renderPlan(data.plan_iteraciones)),
  '{{resguardo_de_documentos_por_tipo_documental}}': multilineToWt(data.resguardo_documentos_por_tipo_documental),
  // Compatibilidad: la fecha puede aparecer también como {{fecha_actual}} en el footer.
  '{{fecha_actual}}':                        escapeXml(data.metadata.fecha_actual_ddmmaa),
};

docXml = applySubstitutions(docXml, subs);

// Post-fix: la plantilla tiene un " }" sobrante tras {{fecha_actual_ddmmaa}}.
// Si tras la sustitución quedó la secuencia "DATE }" en un mismo <w:t>, lo limpiamos.
const dateValue = escapeXml(data.metadata.fecha_actual_ddmmaa);
docXml = docXml.replace(
  new RegExp(dateValue.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + ' \\}', 'g'),
  dateValue,
);

zip.file('word/document.xml', docXml);

// ---------- 4. Footer y cabeceras ----------
const footerSubs = {
  '{{fecha_actual_ddmmaa}}': escapeXml(data.metadata.fecha_actual_ddmmaa),
  '{{fecha_actual}}':        escapeXml(data.metadata.fecha_actual_ddmmaa),
};
for (const part of [
  'word/footer1.xml', 'word/footer2.xml', 'word/footer3.xml',
  'word/header1.xml', 'word/header2.xml', 'word/header3.xml',
]) {
  if (!zip.file(part)) continue;
  let xml = zip.file(part).asText();
  xml = applySubstitutions(xml, footerSubs);
  zip.file(part, xml);
}

// ---------- 5. Validación de placeholders residuales ----------
const checkFiles = [
  'word/document.xml',
  'word/footer1.xml',
  'word/footer2.xml',
  'word/footer3.xml',
  'word/header1.xml',
  'word/header2.xml',
  'word/header3.xml',
];
const leftovers = [];
for (const f of checkFiles) {
  if (!zip.file(f)) continue;
  const xml = zip.file(f).asText();
  const found = xml.match(/\{\{[^}]+\}\}/g);
  if (found) leftovers.push({ file: f, placeholders: found });
}

if (leftovers.length) {
  console.error('ERROR — Placeholders sin sustituir; el archivo .docx NO se escribe:');
  for (const l of leftovers) console.error(' ', l.file, '->', l.placeholders);
  process.exit(5);
}

// ---------- 6. Escribir ----------
const outBuf = zip.generate({ type: 'nodebuffer', compression: 'DEFLATE' });
fs.writeFileSync(ctx.outputDocx, outBuf);
console.log('DDS DOCX generado:', ctx.outputDocx);
console.log('Tamaño:', outBuf.length, 'bytes');
console.log('Todos los placeholders han sido sustituidos correctamente.');
