# tools — Renderizador DDS Santander

Proyecto Java que toma `data/<proyecto>/agente.json` y produce los entregables en `output/<proyecto>/`.

> **El código fuente no está en el repositorio.** El agente Windsurf lo genera la primera vez que se ejecuta el workflow `/generate-dds`, siguiendo las instrucciones de `.windsurf/skills/generate-dds-santander/rendering-guide.md`.

---

## Requisitos

- **Java 17** o superior (`java -version`).
- **Maven 3.8** o superior (`mvn -version`).

---

## Uso

### Generar MD y DOCX

```powershell
java -jar tools/target/dds-tools.jar <proyecto>
```

### Generar solo Markdown

```powershell
java -jar tools/target/dds-tools.jar <proyecto> md
```

### Generar solo Word

```powershell
java -jar tools/target/dds-tools.jar <proyecto> docx
```

Donde `<proyecto>` es el nombre del subdirectorio dentro de `data/` (p. ej. `mi-app`).

---

## Compilar (necesario después de crear o modificar el código fuente)

```powershell
mvn -f tools/pom.xml clean package -q
```

Genera `tools/target/dds-tools.jar`.

---

## Clases del proyecto

| Clase | Responsabilidad |
|---|---|
| `Main` | Punto de entrada. Parsea argumentos, lee `agente.json` y delega en los renderizadores. |
| `PathsHelper` | Resuelve todas las rutas del repositorio (`data/`, `Plantillas/`, `output/`) de forma relativa al directorio de trabajo actual. |
| `RenderMd` | Lee la plantilla `.md`, sustituye placeholders, expande los loops de vocabulario y requisitos, añade el Anexo Técnico y escribe `output/<proyecto>/DDS_<proyecto>.md`. |
| `RenderDocx` | Abre la plantilla `.docx` con Apache POI, sustituye placeholders en párrafos/encabezados/pies, expande tablas de vocabulario y requisitos, valida que no queden `{{...}}` y escribe `output/<proyecto>/DDS_<proyecto>.docx`. |

---

## Salidas esperadas

```
output/<proyecto>/
├── DDS_<proyecto>.md     ← Markdown con Anexo Técnico ampliado
└── DDS_<proyecto>.docx   ← Word fiel a la plantilla Santander
```

El programa imprime en consola:

- `[MD]  Generado: output/<proyecto>/DDS_<proyecto>.md`
- `[DOCX] Todos los placeholders han sido sustituidos correctamente.`

Si aparece `[DOCX] ADVERTENCIA — Placeholders sin sustituir`, el archivo **no se escribe**. Corregir el mapeo en `RenderDocx.java` y recompilar.

---

## Dependencias principales

| Librería | Versión | Uso |
|---|---|---|
| `com.google.code.gson:gson` | 2.10.1 | Parseo de `agente.json` |
| `org.apache.poi:poi-ooxml` | 5.2.5 | Manipulación del `.docx` (OOXML) |

El `pom.xml` usa `maven-shade` para empaquetar un fat JAR con todas las dependencias incluidas.

---

## Regenerar el código fuente

Si se necesita regenerar el código (por cambio de plantilla, nuevos placeholders, etc.):

1. Borrar `tools/src/` y `tools/pom.xml`.
2. Ejecutar `/generate-dds <proyecto>` — el agente detectará que falta el proyecto y lo recreará.

El mapeo completo de placeholders está en `.windsurf/skills/generate-dds-santander/tags-catalog.md`.
