---
description: Da de alta un nuevo proyecto en el pipeline de DDS. Crea las carpetas data/<proyecto>/ y output/<proyecto>/ y produce el esqueleto inicial de agente.json. Invoca con /add-new-project.
---

# Workflow `/add-new-project`

Crea la estructura de carpetas y el esqueleto de `agente.json` para un proyecto nuevo, sin analizar todavía el código. Es el paso previo a `/generate-dds`.

## Entradas

- `<proyecto>`: nombre del proyecto en kebab-case (p. ej. `mi-app-nueva`).
- (Opcional) ruta de un repositorio que se copiará/clonará en `projects/<proyecto>/`.

## Pasos

### 1. Crear las carpetas convencionales
// turbo
```powershell
New-Item -ItemType Directory -Force -Path "projects/<proyecto>", "data/<proyecto>", "output/<proyecto>" | Out-Null
```

### 2. Crear el esqueleto de `agente.json`

Genera `data/<proyecto>/agente.json` con todas las claves del contrato definido en `.windsurf/skills/generate-dds-santander/tags-catalog.md`. Rellena `metadata.*` con los datos básicos conocidos y deja todos los demás campos como `"PENDIENTE_CUESTIONARIO"` o estructuras vacías (`[]`, `{}`) para que se completen en `/generate-dds`.

### 3. (Opcional) Colocar el código fuente

Si el usuario aportó el repositorio:

```powershell
# si es un zip:
Expand-Archive -Path "<ruta-al-zip>" -DestinationPath "projects/<proyecto>" -Force

# si es un repo git:
git clone <url> "projects/<proyecto>"
```

### 4. Anunciar al usuario

Muestra los caminos creados y sugiere ejecutar `/generate-dds <proyecto>` para arrancar el análisis.

## Resultado

```
projects/<proyecto>/   ← código fuente (o vacío, listo para que el usuario lo llene)
data/<proyecto>/agente.json   ← esqueleto vacío
output/<proyecto>/   ← vacío, listo para los entregables
```

