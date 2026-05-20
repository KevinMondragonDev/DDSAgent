---
trigger: model_decision
description: Convenciones de redacción y empaquetado para el DDS Santander. Cargar cuando se trabaja en agente.json, plantillas DDS, scripts de renderizado o se discute el formato Santander.
---

# Convenciones del DDS Santander

## Idioma y tono

- Español neutro formal. Sin regionalismos, salvo que formen parte del dominio reconocido del sistema.
- El DDS es un documento de **negocio**: su audiencia principal son directivos, gestores de proyecto y auditores corporativos, no únicamente desarrolladores.
- Tono formal e impersonal. Las descripciones comunican el valor que el sistema aporta a la organización o al usuario; la referencia técnica complementa, no lidera.

### Jerarquía obligatoria del lenguaje

Toda descripción de componente, funcionalidad o sección debe seguir este orden:

1. **Qué capacidad o beneficio aporta** el sistema (perspectiva de negocio).
2. **Qué hace internamente** el sistema (comportamiento observable).
3. **Cómo lo implementa** la tecnología (detalle técnico como soporte).

**Incorrecto** (tecnología como protagonista):
> "Se utiliza Angular 18 con standalone components para renderizar la vista del tablero."

**Correcto** (valor como protagonista):
> "La interfaz de seguimiento diario refleja en tiempo real el estado de las tareas del usuario sin necesidad de recargar la página; esto se logra mediante la arquitectura de componentes reactivos de Angular 18."

### Construcciones gramaticales obligatorias

- Voz pasiva impersonal o tercera persona del singular: "el sistema permite…", "la solución facilita…", "el módulo garantiza…".
- Los verbos de los requisitos funcionales van en infinitivo: "Registrar…", "Consultar…", "Exportar…".
- Evitar la primera persona singular o plural ("hacemos", "construimos", "nuestra solución").
- Sin lenguaje de marketing, sin calificativos superlativos, sin frases motivacionales.
- Sin emojis ni iconos en ningún campo del DDS.

### Precisión terminológica

- Las versiones de tecnologías se copian exactamente del manifest (`package.json`, `pom.xml`, `requirements.txt`, etc.). No se redondean ni se infieren.
- Los nombres de componentes, servicios y módulos se escriben exactamente como aparecen en el código fuente.
- Los acrónimos se definen en la sección de vocabulario la primera vez que se usan en el cuerpo del documento.

---

## Trazabilidad y manejo de lagunas

- **Cero invención.** Todo dato debe ser verificable en el repositorio del proyecto.
- Si no hay evidencia para un campo:
  1. Asignar en `agente.json` el sentinel literal `"PENDIENTE_CUESTIONARIO"`.
  2. Añadir la pregunta a `data/<proyecto>/cuestionario.md` siguiendo `questionnaire-format.md`.
  3. Listar el campo en `pendientes[]` del JSON.
- **Nunca** rellenar lagunas con texto plausible para que el documento "se vea completo". Es preferible un sentinel visible a un dato inventado.
- Para cada sección relevante, citar la fuente en el campo `evidencia` del JSON con la ruta relativa al repositorio (`projects/<proyecto>/...`).

---

## Estructura del JSON

- El JSON es la **única fuente de verdad**. El `.md` y el `.docx` son artefactos generados a partir de él.
- No editar los `.docx` generados a mano: el cambio se pierde al volver a renderizar.
- Mantener el orden de claves del esqueleto documentado en `tags-catalog.md`.
- Las fechas siempre en formato `DD/MM/YYYY`.
- El campo `metadata.autor` siempre refleja al agente o persona que generó el documento (p. ej., `"Agente IA"`).

---

## Convenciones de identificadores

| Tipo | Formato | Ejemplo |
|---|---|---|
| Funcionalidades | `F-01`, `F-02`, ... | `F-03` |
| Requisitos funcionales | `RF-01`, `RF-02`, ... | `RF-05` |
| Requisitos no funcionales | `RNF-01`, `RNF-02`, ... | `RNF-02` |
| Referencias documentales | `REF-01`, `REF-02`, ... | `REF-04` |
| Preguntas del cuestionario | `Q-01`, `Q-02`, ... | `Q-07` |
| Códigos de proyecto | `MX-<ACRONIMO>-<NNN>` | `MX-PROY-001` |

---

## Plantilla (inmutable)

- `Plantillas/DDS_Plantilla - formato santander.docx` y su par `.md` son **inmutables**.
- Si Santander publica una nueva versión de la plantilla:
  1. Reemplazar el archivo en `Plantillas/`.
  2. Ejecutar `grep -o "{{[^}]*}}"` sobre el `.md` normalizado para listar los nuevos placeholders.
  3. Actualizar `tags-catalog.md` y los scripts en `tools/`.
  4. Actualizar `metadata.version_documento` en los JSON existentes si el cambio estructural lo requiere.

---

## Reglas de entregabilidad

- Un DDS con sentinels `PENDIENTE_CUESTIONARIO` visibles en el `.docx` **no es entregable**. El cuestionario debe cerrarse antes de la entrega.
- Validación previa a la entrega:
  ```powershell
  Select-String -Pattern "PENDIENTE_CUESTIONARIO" -Path "data/<proyecto>/agente.json" -SimpleMatch
  ```
  El comando debe retornar sin resultados.
- Los scripts de renderizado abortan si detectan placeholders `{{…}}` residuales. Si el script reporta error, no entregar hasta resolverlo.

---

## Comandos canónicos de validación

```powershell
# Validar que el JSON parsea correctamente
node -e "JSON.parse(require('fs').readFileSync('data/<proyecto>/agente.json','utf8'));console.log('OK')"

# Detectar sentinels antes de entregar
Select-String -Pattern "PENDIENTE_CUESTIONARIO" -Path "data/<proyecto>/agente.json" -SimpleMatch

# Renderizar
node tools/render-md.js   <proyecto>
node tools/render-docx.js <proyecto>
```

Cualquier desviación de estos comandos debe documentarse en `tools/README.md`.

---

## Anexo 1 (SO/FI/DI/US)

- Los 7 requisitos estructurales (`SO001` a `US001`) son **obligatorios** en `anexo_requisitos[]`.
- No se permite omitirlos. Si no aplican al sistema, marcar `"aplica_al_sistema": false` y justificar.
- La `justificacion` debe citar el componente, archivo o módulo real del código que fundamenta la decisión. No se aceptan afirmaciones genéricas ("no aplica porque es una aplicación pequeña").
