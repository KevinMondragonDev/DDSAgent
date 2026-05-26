# projects/

Esta carpeta contiene el código fuente de los proyectos a documentar. Cada proyecto ocupa un subdirectorio propio.

## Convención de alta

```
projects/
└── <nombre-del-proyecto>/   ← un subdirectorio por proyecto (kebab-case)
    ├── README.md             (si existe, el agente lo lee primero)
    ├── package.json / pom.xml / requirements.txt ...
    └── src/ ...
```

## Cómo agregar un proyecto

```
/add-new-project <nombre-del-proyecto>
```

O manualmente:

```powershell
New-Item -ItemType Directory -Force -Path "projects/<nombre>", "data/<nombre>", "output/<nombre>"
# Copiar el código fuente a projects/<nombre>/
```

Luego iniciar el análisis con:

```
/generate-dds <nombre-del-proyecto>
```

## Reglas

- El agente **solo lee** esta carpeta; nunca modifica el código fuente aquí.
- Los subdirectorios deben usar **kebab-case** (`mi-app-nueva`, no `MiAppNueva`).
- Un mismo nombre de subdirectorio debe coincidir en `projects/`, `data/` y `output/`.
