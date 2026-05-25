# Guía de renderizado

Una vez `data/<proyecto>/agente.json` está validado, esta es la fase mecánica: ejecutar dos scripts y verificar.

## Preparación (una sola vez por máquina)

```powershell
cd tools
mvn clean package
```

Esto compila el código Java y descarga las dependencias (como `gson`). Genera el ejecutable `target/dds-tools.jar`.

## Renderizado

```powershell
# Desde la raíz del repo:
java -jar tools/target/dds-tools.jar <proyecto>
```

Donde `<proyecto>` coincide con el nombre del subdirectorio dentro de `data/`.

Los scripts:

1. Cargan `Plantillas/DDS_Plantilla - formato santander .{md,docx}`.
2. Cargan `data/<proyecto>/agente.json`.
3. Reemplazan los 12 placeholders + el loop de vocabulario.
4. Verifican que no queden `{{…}}`.
5. Escriben el resultado en `output/<proyecto>/`.

## Salidas

```
output/<proyecto>/
├── DDS_<proyecto>.md     ← versión markdown (incluye Anexo Técnico ampliado)
└── DDS_<proyecto>.docx   ← versión Word fiel a la plantilla Santander
```

## Validación automática que ya hacen los scripts

- ✅ JSON parsea sin error.
- ✅ Ningún placeholder `{{…}}` queda en `document.xml` ni en `footer*.xml` ni en `header*.xml`.
- ✅ La tabla de vocabulario tiene `vocabulario.length` filas, no 6.

Si la validación falla, el script imprime los placeholders huérfanos y **no escribe** el archivo final.

## Validación manual recomendada

1. Abrir el `.docx` en Word/LibreOffice/Google Docs. Comprobar:
   - Encabezados y numeración intactos.
   - Tabla de vocabulario expandida (N filas reales).
   - Footer con la fecha correcta.
2. Revisar el `.md` para garantizar que el "Anexo Técnico — Detalle ampliado" tiene contenido en todas sus subsecciones.

## Iteración

Si tras revisar el `.docx` necesitas corregir contenido:

1. **NO** edites el `.docx` directamente. Es un artefacto generado y se sobreescribe.
2. Edita `data/<proyecto>/agente.json`.
3. Vuelve a ejecutar `render-docx.js`.

Esto garantiza que el JSON es la única fuente de verdad.

## Errores comunes y cómo resolverlos

| Síntoma | Causa | Solución |
|---|---|---|
| `JSON syntax error` | comilla suelta o coma sobrante en el JSON | corregir el JSON |
| Aparecen `{{…}}` en el `.docx` | placeholder nuevo en la plantilla no mapeado | añadir el mapeo en `tools/src/main/java/com/santander/dds/RenderDocx.java` y en `tags-catalog.md` |
| Tabla de vocabulario sigue mostrando 6 filas iguales | regex de detección de filas no matchea | inspeccionar el XML de la plantilla y ajustar el patrón en `RenderDocx.java` |
| `.docx` no abre en Word | XML mal formado (caracteres `<`, `>`, `&` sin escapar) | revisar `escapeXml` en el script |
| `Error: Unable to access jarfile` | `mvn package` no se ejecutó | `cd tools && mvn package` |

## Modos de ejecución (Opcional)

Si solo quieres generar un formato, puedes pasar el modo:

```powershell
java -jar tools/target/dds-tools.jar <proyecto> md
java -jar tools/target/dds-tools.jar <proyecto> docx
```
