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

Esta tabla cubre **campo a campo** todo el esqueleto JSON. Generar la pregunta correspondiente en cuanto no haya evidencia verificable en el código. Cada fila es una pregunta independiente en el `cuestionario.md`.

---

### Metadatos del documento

| Campo JSON | Pregunta concreta al usuario | Pista de formato | Bloqueante |
|---|---|---|---|
| `metadata.codigo_proyecto` | ¿Cuál es el código de proyecto Santander asignado? | Formato `MX-<ACRONIMO>-<NNN>`. Ejemplo: `MX-RISK-042`. Consultar con el gestor de proyecto. | Sí |
| `metadata.descripcion_de_los_cambios` | ¿Qué describe esta versión del DDS? (resumen del análisis o del cambio documentado) | Una sola frase. Ejemplo: `"Versión inicial generada por análisis automatizado del repositorio."` | No |
| `metadata.version_documento` | ¿Cuál es el número de versión del documento? | Formato `V1.0`, `V1.1`, `V2.0`, etc. Si es la primera versión, usar `V1.0`. | No |

---

### §1 — Introducción

| Campo JSON | Pregunta concreta al usuario | Pista de formato | Bloqueante |
|---|---|---|---|
| `introduccion` | ¿Cuál es el contexto corporativo del sistema? Describir brevemente para qué área o proceso del negocio existe este sistema. | Párrafo de 3 a 6 oraciones. Tono formal. Ejemplo: `"El sistema X forma parte de la plataforma de…"`. | Sí |
| `proposito` | ¿Cuál es el propósito de negocio del sistema? (no la tecnología, sino el valor que aporta a la organización o al usuario) | Una a tres oraciones. Ejemplo: `"Permitir a los usuarios consultar y registrar…"`. | Sí |
| `alcance` | ¿Qué incluye y qué excluye explícitamente este sistema? | Párrafo indicando límites: qué procesos cubre, qué queda fuera. Ejemplo: `"El sistema cubre… No incluye…"`. | Sí |
| `objetivos_beneficios[]` | Listar los objetivos o beneficios cuantificables que aporta el sistema. | Un objetivo por línea. Ejemplo: `"Reducir el tiempo de registro de tareas en un 40%."` | No |
| `descripcion_del_sistema` | Describir la arquitectura general del sistema: capas, componentes principales, cómo se comunican entre sí y con sistemas externos. | Párrafo técnico de 4 a 8 oraciones. Mencionar tecnologías principales confirmadas en el código. | Sí |

---

### §1.3 — Vocabulario

| Campo JSON | Pregunta concreta al usuario | Pista de formato | Bloqueante |
|---|---|---|---|
| `vocabulario[*].termino` | ¿Cuáles son los términos del dominio o acrónimos que un lector externo no conocería? | Lista de términos con su definición. Uno por línea: `TERMINO: definición`. Incluir entre 8 y 20 entradas. | No |
| `vocabulario[*].definicion` | (Se responde junto con `termino` en el mismo bloque.) | Ver formato de la pregunta anterior. | No |

---

### §1.4 — Referencias

| Campo JSON | Pregunta concreta al usuario | Pista de formato | Bloqueante |
|---|---|---|---|
| `referencias[*].referencia` | ¿Qué documentos externos o internos referencia este DDS (manuales, APIs, normativas, tickets)? | Una referencia por línea: `REF-01: Nombre del documento o URL`. | No |
| `referencias[*].documento` | (Se responde junto con `referencia` en el mismo bloque.) | Ver formato de la pregunta anterior. | No |

---

### §2 — Situación actual

| Campo JSON | Pregunta concreta al usuario | Pista de formato | Bloqueante |
|---|---|---|---|
| `situacion_actual` | ¿En qué estado se encuentra el sistema actualmente? Describir: fase del proyecto, entorno de despliegue, CI/CD, cobertura de pruebas, riesgos conocidos. | Párrafo de 4 a 8 oraciones. Ejemplo: `"El sistema se encuentra en fase MVP. Se despliega en… No cuenta con pipeline de CI/CD. Los riesgos identificados incluyen…"`. | Sí |

---

### §3 — Análisis y requisitos (introducción)

| Campo JSON | Pregunta concreta al usuario | Pista de formato | Bloqueante |
|---|---|---|---|
| `analisis_y_definicion_de_requisitos` | ¿Qué metodología o proceso se siguió para levantar los requisitos del sistema? | Párrafo de 2 a 4 oraciones. Ejemplo: `"Los requisitos fueron levantados mediante entrevistas con los usuarios clave y revisión del backlog. Se clasificaron en funcionales y no funcionales…"`. | No |

---

### §3.1 — Principales conceptos y relaciones

| Campo JSON | Pregunta concreta al usuario | Pista de formato | Bloqueante |
|---|---|---|---|
| `principales_conceptos_relaciones.entidades[*].nombre` | ¿Cuáles son las entidades o conceptos de dominio principales del sistema? | Un nombre por línea. Ejemplo: `"Usuario"`, `"Tarea"`, `"Proyecto"`. | Sí |
| `principales_conceptos_relaciones.entidades[*].descripcion` | Para cada entidad listada, ¿cuál es su rol en el sistema? | Una descripción por entidad, en el mismo orden. Ejemplo: `"Usuario: persona que accede al sistema para gestionar sus tareas diarias."`. | No |
| `principales_conceptos_relaciones.relaciones[]` | ¿Cómo se relacionan las entidades entre sí? | Una relación por línea. Ejemplo: `"Un Usuario tiene muchas Tareas."`, `"Una Tarea pertenece a un único Proyecto."`. | No |

---

### §3.2 — Funcionalidades del sistema

| Campo JSON | Pregunta concreta al usuario | Pista de formato | Bloqueante |
|---|---|---|---|
| `funcionalidades_sistema[*].nombre` | ¿Cuál es el nombre de negocio (no técnico) de cada funcionalidad principal? | Un nombre por funcionalidad. Ejemplo: `"Registro de avance diario"`, `"Exportación de historial"`. No usar nombres de componentes. | Sí |
| `funcionalidades_sistema[*].descripcion` | Para cada funcionalidad, ¿qué valor aporta al usuario u organización? ¿Qué entra y qué produce el sistema? | Una descripción por funcionalidad, en el mismo orden que los nombres. Tono de negocio, no técnico. | Sí |
| `funcionalidades_sistema[*].componentes` | ¿Qué componentes o servicios del código implementan cada funcionalidad? | Nombres exactos de clases, servicios o módulos tal como aparecen en el código. Uno por línea por funcionalidad. | No |
| `funcionalidades_sistema[*].fuente` | ¿Cuál es el archivo principal que implementa cada funcionalidad? | Ruta relativa al repositorio. Ejemplo: `"src/services/task.service.ts"`. | No |

---

### §3.3 — Definición de roles

| Campo JSON | Pregunta concreta al usuario | Pista de formato | Bloqueante |
|---|---|---|---|
| `definicion_roles[*].rol` | ¿Qué roles de usuario existen en el sistema? | Un rol por línea. Ejemplo: `"Administrador"`, `"Usuario estándar"`, `"Auditor"`. Si no hay autenticación, indicar `"Usuario (sin diferenciación de roles)"`. | Sí |
| `definicion_roles[*].responsabilidad` | Para cada rol, ¿cuáles son sus responsabilidades o permisos dentro del sistema? | Una responsabilidad por rol, en el mismo orden. Ejemplo: `"Administrador: gestiona usuarios, configura parámetros del sistema y accede a todos los módulos."`. | Sí |

---

### §3.4 — División del sistema

| Campo JSON | Pregunta concreta al usuario | Pista de formato | Bloqueante |
|---|---|---|---|
| `division_sistema.capas[*].nombre` | ¿Cuáles son las capas o módulos principales en que se divide el sistema? | Un nombre por capa. Ejemplo: `"Capa de presentación"`, `"Capa de negocio"`, `"Capa de datos"`. | No |
| `division_sistema.capas[*].descripcion` | Para cada capa, ¿cuál es su responsabilidad dentro de la arquitectura? | Una descripción por capa, en el mismo orden. Ejemplo: `"Capa de presentación: gestiona la interfaz de usuario y la navegación entre vistas."`. | No |

---

### §3.5 — Caso de uso principal

| Campo JSON | Pregunta concreta al usuario | Pista de formato | Bloqueante |
|---|---|---|---|
| `caso_uso_principal.nombre` | ¿Cuál es el nombre del caso de uso principal del sistema? (el flujo más representativo o crítico) | Una frase corta. Ejemplo: `"Registro y seguimiento del avance diario de tareas"`. | Sí |
| `caso_uso_principal.actor` | ¿Quién inicia el caso de uso principal? | Nombre del rol. Ejemplo: `"Usuario autenticado"`, `"Administrador"`. | Sí |
| `caso_uso_principal.precondicion` | ¿Qué condiciones deben cumplirse antes de que el flujo pueda iniciar? | Una o dos oraciones. Ejemplo: `"El usuario debe haber iniciado sesión. El sistema debe tener datos cargados para la fecha en curso."`. | No |
| `caso_uso_principal.flujo_basico[]` | ¿Cuáles son los pasos del flujo principal del caso de uso, en orden? | Un paso por línea, numerados. Ejemplo: `"1. El usuario accede a la vista principal."`, `"2. El sistema carga el plan del día…"`. Mínimo 4 pasos. | Sí |
| `caso_uso_principal.postcondicion` | ¿Cuál es el estado del sistema tras completar el flujo exitosamente? | Una o dos oraciones. Ejemplo: `"El sistema registra el avance en la base de datos y actualiza el historial del usuario."`. | No |

---

### §4 — Tabla de requisitos

| Campo JSON | Pregunta concreta al usuario | Pista de formato | Bloqueante |
|---|---|---|---|
| `tabla_requisitos[*].id` | ¿Cuáles son los identificadores de los requisitos? | Formato `RF-01`, `RF-02`… para funcionales; `RNF-01`, `RNF-02`… para no funcionales. | Sí |
| `tabla_requisitos[*].tipo` | ¿Cada requisito es Funcional o No Funcional? | Valor exacto: `"Funcional"` o `"No Funcional"`. | Sí |
| `tabla_requisitos[*].requisito` | ¿Cuál es el enunciado de cada requisito? | Verbo en infinitivo para funcionales. Ejemplo: `"Registrar el cumplimiento diario de tareas."`. Para no funcionales: restricción medible. Mínimo 5 requisitos en total. | Sí |
| `tabla_requisitos[*].prioridad` | ¿Cuál es la prioridad de cada requisito? | Valor exacto: `"Alta"`, `"Media"` o `"Baja"`. | Sí |

---

### §5 — XBRL

| Campo JSON | Pregunta concreta al usuario | Pista de formato | Bloqueante |
|---|---|---|---|
| `informes_xbrl` | ¿El sistema genera o consume informes bajo el estándar XBRL? Si no aplica, indicarlo explícitamente. | Párrafo breve. Ejemplo: `"El sistema no genera informes bajo el estándar XBRL. No gestiona datos financieros regulatorios."` o descripción del uso si aplica. | No |

---

### §6 — Documentación a producir

| Campo JSON | Pregunta concreta al usuario | Pista de formato | Bloqueante |
|---|---|---|---|
| `documentacion_a_producir[]` | ¿Qué documentos se producirán o están pendientes de producir para este sistema, además del DDS? | Un documento por línea. Ejemplo: `"Manual de usuario."`, `"Guía de despliegue."`, `"Documentación de la API REST."`. | No |

---

### §7 — Plan de iteraciones

| Campo JSON | Pregunta concreta al usuario | Pista de formato | Bloqueante |
|---|---|---|---|
| `plan_iteraciones[*].iteracion` | ¿Cuántas iteraciones o entregas parciales tiene el proyecto? | Número entero por iteración. Ejemplo: `1`, `2`, `3`. | No |
| `plan_iteraciones[*].nombre` | ¿Cuál es el nombre o etiqueta de cada iteración? | Una frase corta. Ejemplo: `"MVP — funcionalidades básicas"`, `"Iteración 2 — exportación y reportes"`. | No |
| `plan_iteraciones[*].alcance` | ¿Qué funcionalidades o entregables cubre cada iteración? | Una descripción por iteración. Ejemplo: `"Registro de tareas, vista diaria y persistencia local."`. | No |
| `plan_iteraciones[*].estado` | ¿Cuál es el estado actual de cada iteración? | Valor descriptivo: `"Completada"`, `"En curso"`, `"Pendiente"`. | No |

---

### §8 — Resguardo documental

| Campo JSON | Pregunta concreta al usuario | Pista de formato | Bloqueante |
|---|---|---|---|
| `resguardo_documentos_por_tipo_documental` | ¿Cómo gestiona el sistema el resguardo de documentos por tipo documental? Si no aplica, indicarlo. | Párrafo breve. Ejemplo: `"El sistema no gestiona tipos documentales formales. Los archivos generados se almacenan en el sistema de archivos local del usuario."` | No |

---

### Anexo 1 — Requisitos estructurales Santander (SO/FI/DI/US)

Para cada requisito, el agente debe determinar primero si `aplica_al_sistema` es `true` o `false` a partir del código. Solo preguntar la `justificacion` si no hay evidencia suficiente.

| Campo JSON | Pregunta concreta al usuario | Pista de formato | Bloqueante |
|---|---|---|---|
| `anexo_requisitos[SO001].aplica_al_sistema` | ¿El sistema contempla múltiples empresas u organizaciones (multiempresa/multitenant)? | Responder `true` o `false`. | Sí |
| `anexo_requisitos[SO001].justificacion` | Justificar por qué aplica o no aplica SO001 (multiempresa). Citar el componente o módulo relevante. | Ejemplo: `"No aplica: el sistema es de uso personal sin concepto de organización ni tenant. No existe ningún módulo de gestión de empresas en el código."`. | Sí |
| `anexo_requisitos[SO002].aplica_al_sistema` | ¿El sistema maneja valores monetarios en múltiples divisas? | Responder `true` o `false`. | Sí |
| `anexo_requisitos[SO002].justificacion` | Justificar por qué aplica o no aplica SO002 (multidivisa). | Ejemplo: `"No aplica: el sistema no gestiona importes ni transacciones monetarias."`. | Sí |
| `anexo_requisitos[SO003].aplica_al_sistema` | ¿La lógica de negocio está separada de la capa de presentación (independencia de canal)? | Responder `true` o `false`. | Sí |
| `anexo_requisitos[SO003].justificacion` | Justificar citando la estructura de capas del código. | Ejemplo: `"Aplica parcialmente: los servicios de Angular mezclan lógica de negocio con llamadas al DOM en algunos componentes. Ver `src/components/dashboard.component.ts`."`. | Sí |
| `anexo_requisitos[SO004].aplica_al_sistema` | ¿El sistema soporta múltiples idiomas (internacionalización/i18n)? | Responder `true` o `false`. | Sí |
| `anexo_requisitos[SO004].justificacion` | Justificar con referencia a archivos de i18n o ausencia de ellos. | Ejemplo: `"No aplica: no existen archivos de traducción ni configuración i18n en el repositorio. El sistema opera únicamente en español."`. | Sí |
| `anexo_requisitos[FI001].aplica_al_sistema` | ¿El sistema tiene requisito de disponibilidad 24 horas o SLA definido? | Responder `true` o `false`. | Sí |
| `anexo_requisitos[FI001].justificacion` | Justificar con referencia a la arquitectura de despliegue o documentación de SLA. | Ejemplo: `"No aplica: aplicación de uso personal sin SLA definido ni arquitectura de alta disponibilidad."`. | Sí |
| `anexo_requisitos[DI001].aplica_al_sistema` | ¿El sistema se integra con sistemas corporativos del banco (SSO, APIs internas, ERP, etc.)? | Responder `true` o `false`. | Sí |
| `anexo_requisitos[DI001].justificacion` | Justificar citando las integraciones existentes o su ausencia. | Ejemplo: `"No aplica: el sistema no consume ni expone APIs corporativas del banco. Opera de forma aislada."`. | Sí |
| `anexo_requisitos[US001].aplica_al_sistema` | ¿El sistema es una aplicación web accesible desde navegador? | Responder `true` o `false`. | Sí |
| `anexo_requisitos[US001].justificacion` | Justificar con referencia a los navegadores compatibles o indicar que no aplica si es CLI/nativo. | Ejemplo: `"Aplica: aplicación web Angular. Compatible con Chrome 120+, Firefox 121+. No se han ejecutado pruebas en Opera ni IE."`. | Sí |

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
