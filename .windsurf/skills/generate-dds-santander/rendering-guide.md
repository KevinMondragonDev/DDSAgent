# Guía de renderizado

Una vez `data/<proyecto>/agente.json` está validado, esta es la fase mecánica: ejecutar dos scripts y verificar.

## Preparación (una sola vez por máquina)

```powershell
cd tools
npm install
```

Esto instala `pizzip` (~50 KB) en `tools/node_modules/`. No requiere conexión a internet en subsiguientes ejecuciones.

## Renderizado

```powershell
# Desde la raíz del repo:
node tools/render-md.js   <proyecto>     # → output/<proyecto>/DDS_<proyecto>.md
node tools/render-docx.js <proyecto>     # → output/<proyecto>/DDS_<proyecto>.docx
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
| `JSON.parse error` | comilla suelta o coma sobrante en el JSON | corregir el JSON, validar con `node -e "JSON.parse(...)"` |
| Aparecen `{{…}}` en el `.docx` | placeholder nuevo en la plantilla no mapeado | añadir el mapeo en `tools/render-docx.js` y en `tags-catalog.md` |
| Tabla de vocabulario sigue mostrando 6 filas iguales | regex de detección de filas no matchea | inspeccionar el XML de la plantilla y ajustar el patrón en `render-docx.js` |
| `.docx` no abre en Word | XML mal formado (caracteres `<`, `>`, `&` sin escapar) | revisar `escapeXml` en el script; el helper ya lo cubre |
| `Cannot find module 'pizzip'` | `npm install` no se ejecutó | `cd tools && npm install` |

## Scripts npm (atajos opcionales)

Definidos en `tools/package.json`:

```powershell
cd tools
npm run render:md   -- <proyecto>
npm run render:docx -- <proyecto>
```
