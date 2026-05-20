---
name: generate-dds-santander
description: Analiza un proyecto de software y genera un Documento de Diseño del Sistema (DDS) en formato Santander. Cubre los placeholders de la plantilla, produce un JSON estructurado y renderiza tanto Markdown como Word (.docx). Invocar cuando el usuario pida un DDS, un documento de diseño formato Santander, o cuando entregue un repositorio para documentar.
---

# Skill — Generador de DDS Santander

## Propósito

Convertir un repositorio de software (front-end, back-end, monolito o arquitectura distribuida) en un **Documento de Diseño del Sistema (DDS)** corporativo conforme al formato Santander. La skill cubre tres fases secuenciales:

1. **Análisis** del repositorio: código fuente, configuración, datos y documentación existente.
2. **Producción** de `agente.json`: rellena cada placeholder `{{...}}` de la plantilla con información verificable en el código.
3. **Renderizado** de los artefactos finales en `.md` y `.docx` listos para entrega.

## Cuándo invocar esta skill

- El usuario solicita "genera el DDS de este proyecto".
- El usuario menciona "plantilla Santander", "DDS" o "Documento de Diseño del Sistema".
- El usuario entrega un repositorio para que sea documentado en formato corporativo.
- El usuario invoca el workflow `/generate-dds`.

## Estructura del repositorio de trabajo

```
DDSAgent/
├── Plantillas/
│   ├── DDS_Plantilla - formato santander.docx   <- plantilla original (inmutable)
│   └── DDS_Plantilla - formato santander.md     <- misma plantilla en Markdown
├── projects/
│   └── <proyecto>/                              <- código fuente a analizar
├── tools/
│   ├── render-md.js                             <- renderiza MD desde agente.json
│   ├── render-docx.js                           <- renderiza DOCX desde agente.json
│   └── package.json
├── data/
│   └── <proyecto>/agente.json                   <- JSON estructurado por proyecto
└── output/
    └── <proyecto>/                              <- entregables finales
        ├── DDS_<proyecto>.md
        └── DDS_<proyecto>.docx
```

Si el proyecto aún no cuenta con esta estructura, crearla antes de proceder.

## Procedimiento (3 fases)

### Fase 1 — Análisis del repositorio

Sigue la checklist en `analysis-guide.md`. Recolecta únicamente evidencia verificable en el código y construye los siguientes hechos en memoria:

- Nombre del proyecto, código corporativo (`MX-…`), versión.
- Stack tecnológico: lenguaje, framework principal, gestor de paquetes, persistencia e integraciones.
- Componentes, módulos o servicios principales con su ruta en el repositorio.
- Entidades de dominio y sus relaciones.
- Funcionalidades extremo a extremo: qué capacidad aporta el sistema y qué hace internamente.
- Roles del sistema y sus responsabilidades.
- Estado actual del despliegue (MVP, producción, prototipo, sin backend, etc.).
- Idiomas, navegadores y plataformas soportados.

### Fase 2 — Producción de `agente.json` + `cuestionario.md`

Consulta `tags-catalog.md` para conocer **todas las claves obligatorias**. Escribe el JSON en `data/<proyecto>/agente.json`. Reglas irrenunciables:

1. **Cero invención.** Si un dato no tiene evidencia en el código:
   - Asigna al campo el valor sentinel literal `"PENDIENTE_CUESTIONARIO"` (no inventar, no dejar vacío, no escribir texto plausible).
   - Añade la pregunta correspondiente a `data/<proyecto>/cuestionario.md` siguiendo `questionnaire-format.md`.
   - Lista la entrada en `pendientes[]` del JSON.
2. **Tono formal orientado al negocio.** El DDS es un documento corporativo leído por directivos, gestores de proyecto y auditores. Cada descripción debe responder primero "qué capacidad o valor aporta el sistema" y únicamente después "cómo lo implementa la tecnología". Consulta `dds-conventions.md` para las reglas de redacción completas.
3. **Trazabilidad.** Popula el campo `evidencia` con las rutas relativas al repositorio de cada hallazgo relevante.
4. **Idioma.** Español neutro técnico. Sin mezclar idiomas ni usar regionalismos que no pertenezcan al dominio del sistema.
5. **Validación JSON.** Antes de renderizar, ejecuta:
   ```powershell
   node -e "JSON.parse(require('fs').readFileSync('data/<proyecto>/agente.json','utf8'));console.log('OK')"
   ```
6. **Aviso de cuestionario.** Si se generó un cuestionario, informa al usuario antes de renderizar; es probable que prefiera responder primero para evitar entregar un documento con sentinels visibles.

### Fase 3 — Renderizado

Consulta `rendering-guide.md` para el detalle completo. Comandos canónicos:

```powershell
cd tools
npm install           # solo la primera vez en el equipo
node render-md.js   <proyecto>   # genera output/<proyecto>/DDS_<proyecto>.md
node render-docx.js <proyecto>   # genera output/<proyecto>/DDS_<proyecto>.docx
```

Validación obligatoria al terminar: ningún `{{…}}` debe quedar en el `.docx`. Los scripts abortan la escritura si detectan placeholders residuales.

## Recursos de esta skill

- **`tags-catalog.md`** — Contrato completo entre los placeholders de la plantilla y las claves del JSON.
- **`analysis-guide.md`** — Checklist de análisis del repositorio: qué leer y a qué clave alimenta cada hallazgo.
- **`rendering-guide.md`** — Cómo ejecutar los scripts, validar y corregir errores de renderizado.
- **`questionnaire-format.md`** — Formato canónico del `cuestionario.md` para gestionar lagunas sin inventar datos.

## Buenas prácticas

- Usar `todo_write` con un máximo de seis ítems y marcarlos progresivamente conforme se avance.
- Antes de redactar cualquier contenido, buscar evidencia con las herramientas de búsqueda (`grep`, `glob`).
- Si el repositorio tiene un `README.md` o un briefing del propietario, leerlo en primer lugar; habitualmente contiene el propósito y el alcance.
- No modificar la carpeta `Plantillas/`. Esa carpeta es inmutable.
- Si Santander publica una nueva versión de la plantilla, actualizar `tags-catalog.md`, los scripts de `tools/` y el esqueleto del JSON en una sola iteración controlada.

## Errores frecuentes que deben evitarse

- Mezclar idiomas (español e inglés) dentro de un mismo párrafo.
- Declarar tecnologías o componentes que no constan en `package.json`, `pom.xml`, `requirements.txt` u otro manifest verificable.
- **Rellenar lagunas con texto plausible.** Si no hay evidencia, se usa el sentinel y se genera la pregunta en el cuestionario; nunca se inventa.
- Omitir el Anexo 1 (SO001..US001). Cada requisito exige `aplica_al_sistema` y `justificacion` con base en el código real.
- Entregar un `.docx` con sentinels `PENDIENTE_CUESTIONARIO` visibles. El cuestionario debe cerrarse primero.
- Entregar un `.docx` sin verificar que no quedan placeholders `{{…}}` sin sustituir.
