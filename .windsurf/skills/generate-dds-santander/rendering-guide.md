# Guía de renderizado

Una vez `data/<proyecto>/agente.json` está validado, esta guía cubre dos situaciones:

- **Primera ejecución en el equipo:** el proyecto Java aún no existe en `tools/` → el agente debe crearlo (Fase A) antes de compilar.
- **Ejecuciones posteriores:** `tools/pom.xml` ya existe → saltar directamente a la Fase B.

> El código fuente Java **no se versiona en el repositorio**. El agente lo genera en `tools/` durante la primera ejecución a partir de las especificaciones de esta guía.

---

## Fase A — Crear el proyecto Java (solo si no existe)

### A.0 Verificar si ya existe

```powershell
Test-Path tools/pom.xml
```

Si devuelve `True`, ir directamente a **Fase B**. Si devuelve `False`, ejecutar los pasos A.1 a A.6.

### A.1 Crear la estructura de directorios

```powershell
New-Item -ItemType Directory -Force -Path "tools/src/main/java/com/santander/dds" | Out-Null
```

### A.2 Crear `tools/pom.xml`

Descriptor Maven del proyecto. Debe contener:

- `groupId`: `com.santander.dds`, `artifactId`: `dds-tools`, `version`: `1.0.0`.
- Java 17 como `source` y `target` del compilador.
- Dependencia **Gson 2.10.1** (`com.google.code.gson`) para parsear `agente.json`.
- Dependencia **Apache POI OOXML 5.2.5** (`org.apache.poi:poi-ooxml`) para manipular el `.docx`.
- Plugin **maven-shade 3.5.1** configurado para generar un fat JAR con `mainClass = com.santander.dds.Main`, incluyendo el transformer `ServicesResourceTransformer` (requerido por POI) y excluyendo firmas `META-INF/*.SF/DSA/RSA`.

### A.3 Crear `PathsHelper.java`

Clase utilitaria que resuelve todas las rutas del repositorio a partir del directorio de trabajo actual (`Paths.get("").toAbsolutePath()`). Debe exponer métodos que devuelvan:

- Ruta al `agente.json` del proyecto (`data/<proyecto>/agente.json`).
- Ruta a la plantilla Markdown (`Plantillas/DDS_Plantilla - formato santander.md`).
- Ruta a la plantilla DOCX (`Plantillas/DDS_Plantilla - formato santander.docx`).
- Ruta al directorio de salida (`output/<proyecto>/`).
- Rutas a los archivos de salida (`output/<proyecto>/DDS_<proyecto>.md` y `.docx`).

### A.4 Crear `Main.java`

Punto de entrada del JAR. Responsabilidades:

- Validar que se pasa al menos un argumento (nombre del proyecto); si no, imprimir uso y salir con código 1.
- Aceptar un segundo argumento opcional: `md`, `docx` o ninguno (ambos).
- Leer y parsear `data/<proyecto>/agente.json` con Gson como `JsonObject`.
- Crear una instancia de `PathsHelper` con el nombre del proyecto.
- Crear el directorio `output/<proyecto>/` si no existe.
- Llamar a `RenderMd.render(...)` y/o `RenderDocx.render(...)` según el modo.
- Capturar excepciones y mostrar mensaje de error claro antes de salir con código 1.

### A.5 Crear `RenderMd.java`

Renderizador del Markdown. Responsabilidades:

- Leer la plantilla `Plantillas/DDS_Plantilla - formato santander.md` en UTF-8.
- Normalizar los escapes de Google Docs en los nombres de placeholder (`\_` → `_`, `\*` → `*`).
- Sustituir todos los placeholders `{{...}}` listados en `tags-catalog.md` por los valores del JSON. El mapeo exacto placeholder → clave JSON está definido en la sección "Contrato plantilla ↔ JSON" de `tags-catalog.md`.
- **Loop de vocabulario:** reemplazar el bloque de 6 filas `{{#vocabulario}}` / `{{termino}}` por tantas filas Markdown reales como entradas tenga `vocabulario[]`.
- **Loop de requisitos:** reemplazar el bloque de 5 filas `{{id_req}}` / `{{tipo_req}}` / `{{descripcion_req}}` / `{{prioridad_req}}` por las filas del array `tabla_requisitos[]`.
- Renderizar los campos de tipo array/objeto (funcionalidades, roles, división, caso de uso, plan) como texto Markdown estructurado antes de sustituir.
- Añadir al final del documento un **Anexo Técnico** con: tabla Anexo 1 (SO001–US001 con `aplica_al_sistema` y `justificacion`), lista de `pendientes[]` y tabla de `evidencia`.
- **Validación:** buscar con regex si quedan `{{...}}` en el resultado; si los hay, imprimirlos como advertencia (no abortar).
- Escribir el resultado en `output/<proyecto>/DDS_<proyecto>.md`.

### A.6 Crear `RenderDocx.java`

Renderizador del Word. Responsabilidades:

- Abrir la plantilla `Plantillas/DDS_Plantilla - formato santander.docx` con Apache POI (`XWPFDocument`).
- Construir un mapa `placeholder → valor` con los mismos pares que `RenderMd` (incluyendo variantes con `\_` para escapes de Google Docs).
- Recorrer todos los **párrafos** del documento, encabezados (`XWPFHeader`) y pies de página (`XWPFFooter`); para cada párrafo que contenga un placeholder, limpiar sus runs y reescribir el texto sustituido.
- Recorrer todas las **tablas** (`XWPFTable`):
  - Detectar filas que contienen `{{#vocabulario}}` y expandirlas en N filas reales con `XWPFTableRow` nuevas.
  - Detectar filas que contienen `{{id_req}}` y expandirlas con los requisitos del JSON.
  - Aplicar el mapa de sustituciones al resto de celdas.
- **Validación:** tras todas las sustituciones, recorrer párrafos y celdas buscando `{{...}}` residuales. Si los hay, imprimirlos con `[DOCX] ADVERTENCIA` y **no escribir** el archivo. Si no hay, imprimir `[DOCX] Todos los placeholders han sido sustituidos correctamente.`
- Escribir el resultado en `output/<proyecto>/DDS_<proyecto>.docx`.

---

## Fase B — Compilar

```powershell
mvn -f tools/pom.xml clean package -q
```

Esto descarga dependencias y genera `tools/target/dds-tools.jar`.

---

## Fase C — Renderizar

```powershell
# Desde la raíz del repo — genera MD y DOCX:
java -jar tools/target/dds-tools.jar <proyecto>

# Solo Markdown:
java -jar tools/target/dds-tools.jar <proyecto> md

# Solo Word:
java -jar tools/target/dds-tools.jar <proyecto> docx
```

Salida esperada en consola: `[DOCX] Todos los placeholders han sido sustituidos correctamente.`

---

## Salidas

```
output/<proyecto>/
├── DDS_<proyecto>.md     ← versión Markdown con Anexo Técnico ampliado
└── DDS_<proyecto>.docx   ← versión Word fiel a la plantilla Santander
```

---

## Validación manual recomendada

1. Abrir el `.docx` en Word/LibreOffice. Comprobar:
   - Tabla "Vocabulario" expandida (N filas reales, no 6).
   - Tabla de requisitos expandida.
   - Footer con la fecha correcta.
   - Cero apariciones de `PENDIENTE_CUESTIONARIO` ni `{{...}}`.
2. Revisar el `.md` para garantizar que el Anexo Técnico tiene contenido en todas sus subsecciones.

---

## Iteración sobre un DDS ya generado

1. **No** editar el `.docx` directamente; es un artefacto generado.
2. Editar `data/<proyecto>/agente.json`.
3. Volver a ejecutar la Fase C.

---

## Errores comunes y cómo resolverlos

| Síntoma | Causa | Solución |
|---|---|---|
| `JSON syntax error` | Comilla suelta o coma sobrante en el JSON | Corregir el JSON |
| Aparecen `{{…}}` en el `.docx` | Placeholder nuevo en la plantilla no mapeado | Añadir el mapeo en `RenderDocx.java` y en `tags-catalog.md` |
| Tabla de vocabulario sigue mostrando 6 filas | La regex de detección de filas no hace match | Inspeccionar el XML de la plantilla (descomprimir el `.docx`) y ajustar la detección en `RenderDocx.java` |
| `.docx` no abre en Word | XML mal formado (caracteres `<`, `>`, `&` sin escapar en el JSON) | Asegurarse de que `RenderDocx` escapa el XML correctamente al insertar texto en celdas/párrafos |
| `Error: Unable to access jarfile` | `mvn package` no se ejecutó o falló | Revisar la salida de Maven; asegurarse de que Java 17+ y Maven 3.8+ están disponibles |
| `ClassNotFoundException` al ejecutar el JAR | El fat JAR no incluyó las dependencias | Verificar que el plugin `maven-shade` está configurado en `pom.xml` y re-ejecutar `mvn package` |
