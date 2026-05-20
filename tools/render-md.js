#!/usr/bin/env node
/**
 * Renderiza el DDS Santander en Markdown.
 *
 * Uso:
 *   node tools/render-md.js <proyecto>
 *
 * Lee:
 *   Plantillas/DDS_Plantilla - formato santander.md
 *   data/<proyecto>/agente.json
 *
 * Escribe:
 *   output/<proyecto>/DDS_<proyecto>.md
 */
const fs = require('fs');
const { resolveProject, loadData } = require('./paths');

const ctx = resolveProject(process.argv[2]);
const data = loadData(ctx.jsonPath);

let tpl = fs.readFileSync(ctx.templateMd, 'utf8');

// ---------- normalización de escapes de Google Docs ----------
// La plantilla exportada desde Google Docs introduce \_ y \# como escapes Markdown.
// También aparecen * actuando como delimitadores de cursiva dentro de placeholders.
// Paso 1: eliminar \_ y \# globales.
tpl = tpl.replace(/\\_/g, '_').replace(/\\#/g, '#');
// Paso 2: normalizar * dentro de cada placeholder {{...}}.
// Google Docs convierte _palabra_ en *palabra* (cursiva). Dentro de un placeholder
// los * actúan como separadores de palabra (equivalentes a _). Se reemplazan por _
// y se deduplican para evitar __ dobles cuando ya hay _ adyacentes.
tpl = tpl.replace(/\{\{([^}]*?)\}\}/g, (_, inner) => {
  const clean = inner.replace(/\*/g, '_').replace(/__+/g, '_').replace(/^_|_$/g, '');
  return `{{${clean}}}`;
});

// ---------- renderizadores de secciones ----------

const renderVocabulario = (items) =>
  '| Término | Definición |\n| ----- | ----- |\n' +
  items.map((v) => `| ${v.termino} | ${v.definicion} |`).join('\n');

const renderReferencias = (items) =>
  '| Referencia | Documento |\n| ----- | ----- |\n' +
  items.map((r) => `| ${r.referencia} | ${r.documento} |`).join('\n');

const renderObjetivos = (items) => items.map((o) => `- ${o}`).join('\n');

const renderConceptos = (pcr) => {
  const tabla =
    '| Entidad | Descripción |\n| ----- | ----- |\n' +
    pcr.entidades.map((e) => `| ${e.nombre} | ${e.descripcion} |`).join('\n');
  const relaciones =
    pcr.relaciones && pcr.relaciones.length
      ? '\n\n**Relaciones:**\n\n' + pcr.relaciones.map((r) => `- ${r}`).join('\n')
      : '';
  return tabla + relaciones;
};

const renderFuncionalidades = (items) =>
  items
    .map(
      (f) =>
        `**${f.id} — ${f.nombre}**\n\n` +
        `${f.descripcion}\n\n` +
        `- Componentes: ${f.componentes.join(', ')}\n` +
        `- Fuente: \`${f.fuente}\``,
    )
    .join('\n\n---\n\n');

const renderRoles = (items) =>
  '| Rol | Responsabilidad |\n| ----- | ----- |\n' +
  items.map((r) => `| ${r.rol} | ${r.responsabilidad} |`).join('\n');

const renderCapas = (capas) =>
  '| Capa | Descripción |\n| ----- | ----- |\n' +
  capas.map((c) => `| ${c.nombre} | ${c.descripcion} |`).join('\n');

const renderCasoUso = (cu) =>
  `**Caso de uso:** ${cu.nombre}\n\n` +
  `- **Actor:** ${cu.actor}\n` +
  `- **Precondición:** ${cu.precondicion}\n\n` +
  `**Flujo básico:**\n\n` +
  cu.flujo_basico.map((p, i) => `${i + 1}. ${p}`).join('\n') +
  `\n\n**Postcondición:** ${cu.postcondicion}`;

const renderTablaRequisitos = (items) =>
  '| ID | Tipo | Descripción del Requisito | Prioridad |\n| ----- | ----- | ----- | ----- |\n' +
  items.map((r) => `| ${r.id} | ${r.tipo} | ${r.requisito} | ${r.prioridad} |`).join('\n');

const renderAnexo = (items) =>
  '| Identificador | Requisito | Categoría | Prioridad | Aplica | Justificación |\n' +
  '| ----- | ----- | ----- | ----- | ----- | ----- |\n' +
  items
    .map(
      (r) =>
        `| ${r.identificador} | ${r.requisito} | ${r.categoria} | ${r.prioridad} | ${r.aplica_al_sistema ? 'Sí' : 'No'} | ${r.justificacion} |`,
    )
    .join('\n');

const renderDocs = (items) => items.map((d) => `- ${d}`).join('\n');
const renderPendientes = (items) => items.map((p) => `- ${p}`).join('\n');

const renderPlan = (items) =>
  '| # | Nombre | Alcance | Estado |\n| ----- | ----- | ----- | ----- |\n' +
  items.map((i) => `| ${i.iteracion} | ${i.nombre} | ${i.alcance} | ${i.estado} |`).join('\n');

// ---------- sustituciones 1:1 ----------
const simple = {
  fecha_actual_ddmmaa:   data.metadata.fecha_actual_ddmmaa,
  descripcion_de_los_cambios: data.metadata.descripcion_de_los_cambios,
  codigo_del_proyecto:   data.metadata.codigo_proyecto,
  autor:                 data.metadata.autor,
  introduccion:          data.introduccion,
  proposito:             data.proposito,
  alcance:               data.alcance,
  'objetivos/beneficios': renderObjetivos(data.objetivos_beneficios),
  descripcion:           data.descripcion_del_sistema,
  situacion_actual:      data.situacion_actual,
  analisis_y_definicion_de_requisitos: data.analisis_y_definicion_de_requisitos,
  Principales_conceptos_y_relaciones_entre_ellos: renderConceptos(data.principales_conceptos_relaciones),
  funcionalidades_sistema: renderFuncionalidades(data.funcionalidades_sistema),
  definicion_de_roles:   renderRoles(data.definicion_roles),
  division_del_sistema:  renderCapas(data.division_sistema.capas),
  caso_de_uso_principal: renderCasoUso(data.caso_uso_principal),
  'Informe_bajo_el_estándar_Xbrl': data.informes_xbrl,
  documentacion_a_producir: renderDocs(data.documentacion_a_producir),
  plan_de_iteraciones:   renderPlan(data.plan_iteraciones),
  resguardo_de_documentos_por_tipo_documental: data.resguardo_documentos_por_tipo_documental,
};

for (const [k, v] of Object.entries(simple)) {
  const escaped = k.replace(/[.*+?^${}()|[\]\\/]/g, '\\$&');
  const re = new RegExp(`\\{\\{\\s*${escaped}\\s*\\}\\}`, 'g');
  tpl = tpl.replace(re, String(v));
}

// ---------- tabla de vocabulario (loop) ----------
// La plantilla tiene 6 filas plantilla con *{{#vocabulario}}* | *{{termino}}*.
// Después de la normalización quedan como {{#vocabulario}} | {{termino}}.
tpl = tpl.replace(
  /\| Término \| Definición \|[\s\S]*?\| \*?\{\{#vocabulario\}\}\* ?\| \*?\{\{termino\}\}\*?[^\n]*\n(?:\| \*?\{\{#vocabulario\}\}\* ?\| \*?\{\{termino\}\}\*?[^\n]*\n)+/,
  renderVocabulario(data.vocabulario) + '\n',
);

// ---------- tabla de referencias (filas vacías) ----------
tpl = tpl.replace(
  /\| Referencia \| Documento \|\n\| ----- \| ----- \|\n(?:\|\s*\|\s*\|\n)+/,
  renderReferencias(data.referencias) + '\n',
);

// ---------- tabla de requisitos (loop fila a fila) ----------
// La plantilla tiene 5 filas con {{id_req}} | {{tipo_req}} | {{descripcion_req}} | {{prioridad_req}}.
tpl = tpl.replace(
  /\| ID \| Tipo \| Descripción del Requisito \| Prioridad \|[\s\S]*?(?=\n#|\n\*\*Anexo|$)/,
  renderTablaRequisitos(data.tabla_requisitos) + '\n',
);

// ---------- Anexo 1 (Requisitos estructurales SO/FI/DI/US) ----------
tpl = tpl.replace(
  /\*\*Anexo 1: Lista de Requisitos a satisfacer por todas las aplicaciones\*\*[\s\S]*$/,
  `**Anexo 1: Lista de Requisitos a satisfacer por todas las aplicaciones**\n\n` +
    `Todas las aplicaciones y sistemas desarrollados cumplirán los requisitos estructurales descritos en este anexo.\n\n` +
    renderAnexo(data.anexo_requisitos) +
    '\n\n---\n\n' +
    `## Apéndice técnico — Pendientes y trazabilidad\n\n` +
    `### Pendientes\n\n` +
    renderPendientes(data.pendientes) +
    '\n',
);

// ---------- validación ----------
const remaining = tpl.match(/\{\{[^}]+\}\}/g);
if (remaining) {
  console.warn('AVISO — Placeholders sin sustituir:', remaining);
}

fs.writeFileSync(ctx.outputMd, tpl, 'utf8');
console.log('DDS Markdown generado:', ctx.outputMd);
console.log('Tamaño:', tpl.length, 'bytes');
