# Catálogo de placeholders y mapeo al JSON

La plantilla `Plantillas/DDS_Plantilla - formato santander.md` contiene los placeholders
que se listan a continuación. Cada uno debe tener una clave correspondiente en
`data/<proyecto>/agente.json`.

> **Nota sobre escapes de Google Docs.** La plantilla exportada desde Google Docs introduce
> `\_` y `*` dentro de los nombres de placeholder (p. ej., `{{fecha\_actual\_ddmmaa}}`).
> Los scripts de renderizado normalizan estos escapes automáticamente antes de realizar
> las sustituciones; el catálogo muestra siempre el nombre normalizado (sin escapes).

## Contrato plantilla ↔ JSON

### Control de versiones del documento

| Placeholder en plantilla (normalizado) | Clave en `agente.json` | Tipo | Sección |
|---|---|---|---|
| `{{fecha_actual_ddmmaa}}` | `metadata.fecha_actual_ddmmaa` | `"DD/MM/YYYY"` | Control de Versiones |
| `{{descripcion_de_los_cambios}}` | `metadata.descripcion_de_los_cambios` | `string` | Control de Versiones |
| `{{codigo_del_proyecto}}` | `metadata.codigo_proyecto` | `string` | Control de Versiones |
| `{{autor}}` | `metadata.autor` | `string` | Control de Versiones |

### Cuerpo del documento

| Placeholder en plantilla (normalizado) | Clave en `agente.json` | Tipo | Sección DDS |
|---|---|---|---|
| `{{introduccion}}` | `introduccion` | `string` multilínea | 1. Introducción |
| `{{proposito}}` | `proposito` | `string` | 1.1 Propósito |
| `{{alcance}}` | `alcance` | `string` | 1.2 Alcance |
| `{{objetivos/beneficios}}` | `objetivos_beneficios` | `string[]` | 1.2.1 Objetivos |
| `{{descripcion}}` | `descripcion_del_sistema` | `string` multilínea | 1.2.2 Descripción |
| `{{#vocabulario}}` / `{{termino}}` | `vocabulario[]` | `{termino, definicion}[]` | 1.3 Vocabulario (loop) |
| `{{situacion_actual}}` | `situacion_actual` | `string` multilínea | 2. Situación actual |
| `{{analisis_y_definicion_de_requisitos}}` | `analisis_y_definicion_de_requisitos` | `string` | 3. Introducción Análisis |
| `{{Principales_conceptos_y_relaciones_entre_ellos}}` | `principales_conceptos_relaciones` | `{entidades[], relaciones[]}` | 3.1 Conceptos |
| `{{funcionalidades_sistema}}` | `funcionalidades_sistema` | `Funcionalidad[]` | 3.2 Funcionalidades |
| `{{definicion_de_roles}}` | `definicion_roles` | `{rol, responsabilidad}[]` | 3.3 Roles |
| `{{division_del_sistema}}` | `division_sistema` | `{capas[]}` | 3.4 División |
| `{{caso_de_uso_principal}}` | `caso_uso_principal` | `{nombre, actor, ...}` | 3.5 Caso de uso |
| `{{id_req}}` / `{{tipo_req}}` / `{{descripcion_req}}` / `{{prioridad_req}}` | `tabla_requisitos[]` | `{id, tipo, requisito, prioridad}[]` | 4. Tabla de requisitos (loop) |
| `{{Informe_bajo_el_estándar_Xbrl}}` | `informes_xbrl` | `string` | 5. XBRL |
| `{{documentacion_a_producir}}` | `documentacion_a_producir` | `string[]` | 6. Documentación |
| `{{plan_de_iteraciones}}` | `plan_iteraciones` | `{iteracion, nombre, alcance, estado}[]` | 7. Plan |
| `{{resguardo_de_documentos_por_tipo_documental}}` | `resguardo_documentos_por_tipo_documental` | `string` | 8. Resguardo |

> El placeholder `{{fecha_actual}}` también puede aparecer en los pies de página del `.docx`.
> Los scripts lo mapean automáticamente a `metadata.fecha_actual_ddmmaa`.

## Loop de vocabulario

La plantilla repite 6 filas con el par `{{#vocabulario}}` | `{{termino}}`. El renderizador
las **expande** a tantas filas como entradas tenga `vocabulario[]` en el JSON.

- Columna 1 (`{{#vocabulario}}`) ← `vocabulario[i].termino`
- Columna 2 (`{{termino}}`) ← `vocabulario[i].definicion`

## Loop de tabla de requisitos

La plantilla repite 5 filas con `{{id_req}}`, `{{tipo_req}}`, `{{descripcion_req}}`,
`{{prioridad_req}}`. El renderizador las **expande** a tantas filas como entradas haya
en `tabla_requisitos[]`.

## Claves adicionales del JSON sin placeholder directo en la plantilla

Estas claves son obligatorias para la trazabilidad y se incluyen en el Apéndice técnico
del `.md`. No aparecen en el `.docx` para preservar la fidelidad visual de la plantilla.

| Clave | Tipo | Contenido |
|---|---|---|
| `referencias` | `{referencia, documento}[]` | Archivos clave del repositorio |
| `pendientes` | `string[]` | Lagunas no resueltas y TODOs detectados |
| `anexo_requisitos` | `Requisito[]` | SO001..US001 con `aplica_al_sistema` y `justificacion` |
| `evidencia` | `{alias: ruta}` | Mapa alias → ruta relativa al repositorio |

## Esqueleto mínimo del JSON

```json
{
  "metadata": {
    "proyecto": "<Nombre del proyecto>",
    "codigo_proyecto": "MX-<ACRONIMO>-<NNN>",
    "version_documento": "V1.0",
    "fecha_actual_ddmmaa": "DD/MM/YYYY",
    "descripcion_de_los_cambios": "<resumen del análisis>",
    "autor": "Agente IA",
    "plantilla_origen": "Plantillas/DDS_Plantilla - formato santander.md",
    "repositorio_analizado": "projects/<proyecto>/"
  },
  "introduccion": "PENDIENTE_CUESTIONARIO",
  "proposito": "PENDIENTE_CUESTIONARIO",
  "alcance": "PENDIENTE_CUESTIONARIO",
  "objetivos_beneficios": [],
  "descripcion_del_sistema": "PENDIENTE_CUESTIONARIO",
  "vocabulario": [{ "termino": "", "definicion": "" }],
  "referencias": [{ "referencia": "REF-01", "documento": "" }],
  "situacion_actual": "PENDIENTE_CUESTIONARIO",
  "analisis_y_definicion_de_requisitos": "PENDIENTE_CUESTIONARIO",
  "principales_conceptos_relaciones": {
    "entidades": [{ "nombre": "", "descripcion": "" }],
    "relaciones": []
  },
  "funcionalidades_sistema": [
    { "id": "F-01", "nombre": "", "descripcion": "", "componentes": [], "fuente": "" }
  ],
  "definicion_roles": [{ "rol": "", "responsabilidad": "" }],
  "division_sistema": {
    "capas": [{ "nombre": "", "descripcion": "" }]
  },
  "caso_uso_principal": {
    "nombre": "",
    "actor": "",
    "precondicion": "",
    "flujo_basico": [],
    "postcondicion": ""
  },
  "tabla_requisitos": [
    { "id": "RF-01", "tipo": "Funcional", "requisito": "", "prioridad": "Alta" }
  ],
  "informes_xbrl": "No aplica.",
  "documentacion_a_producir": ["DDS — el presente documento."],
  "resguardo_documentos_por_tipo_documental": "PENDIENTE_CUESTIONARIO",
  "plan_iteraciones": [
    { "iteracion": 1, "nombre": "", "alcance": "", "estado": "Pendiente" }
  ],
  "anexo_requisitos": [
    {
      "identificador": "SO001",
      "requisito": "La aplicación ha de ser multiempresa.",
      "categoria": "Soporte",
      "prioridad": "Alta",
      "fecha": "20-12-2001",
      "aplica_al_sistema": false,
      "justificacion": "PENDIENTE_CUESTIONARIO"
    },
    {
      "identificador": "SO002",
      "requisito": "La aplicación ha de ser multidivisa.",
      "categoria": "Soporte",
      "prioridad": "Alta",
      "fecha": "20-12-2001",
      "aplica_al_sistema": false,
      "justificacion": "PENDIENTE_CUESTIONARIO"
    },
    {
      "identificador": "SO003",
      "requisito": "El análisis de la lógica de negocio de la aplicación ha de ser independiente del canal.",
      "categoria": "Soporte",
      "prioridad": "Alta",
      "fecha": "20-12-2001",
      "aplica_al_sistema": false,
      "justificacion": "PENDIENTE_CUESTIONARIO"
    },
    {
      "identificador": "SO004",
      "requisito": "La aplicación ha de ser multilengua.",
      "categoria": "Soporte",
      "prioridad": "Alta",
      "fecha": "20-12-2001",
      "aplica_al_sistema": false,
      "justificacion": "PENDIENTE_CUESTIONARIO"
    },
    {
      "identificador": "FI001",
      "requisito": "La aplicación ha de ser 24 horas.",
      "categoria": "Fiabilidad",
      "prioridad": "Alta",
      "fecha": "20-12-2001",
      "aplica_al_sistema": false,
      "justificacion": "PENDIENTE_CUESTIONARIO"
    },
    {
      "identificador": "DI001",
      "requisito": "Integración en los sistemas corporativos del banco.",
      "categoria": "Restricciones de Diseño",
      "prioridad": "Alta",
      "fecha": "20-12-2001",
      "aplica_al_sistema": false,
      "justificacion": "PENDIENTE_CUESTIONARIO"
    },
    {
      "identificador": "US001",
      "requisito": "Compatibilidad de navegadores en aplicaciones de Internet (IE, Firefox, Opera).",
      "categoria": "Usabilidad",
      "prioridad": "Alta",
      "fecha": "20-12-2001",
      "aplica_al_sistema": false,
      "justificacion": "PENDIENTE_CUESTIONARIO"
    }
  ],
  "pendientes": [],
  "evidencia": {}
}
```
