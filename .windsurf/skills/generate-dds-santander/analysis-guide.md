# Guía de análisis del repositorio

Sigue esta checklist en orden. Para cada bloque se indica qué archivos del proyecto leer
y a qué clave de `agente.json` alimenta el hallazgo.

> **Principio rector de redacción:** el DDS es un documento de negocio. Toda descripción
> debe responder primero *qué capacidad o beneficio aporta el sistema* y únicamente después
> *qué tecnología lo hace posible*. Nunca al revés. La tecnología es el medio; el valor
> para la organización o el usuario es el fin.

---

## 0. Reconocimiento inicial (5 minutos)

| Acción | Herramienta / archivo a leer | Clave alimentada |
|---|---|---|
| Listar la raíz del proyecto | `list_dir projects/<proyecto>` | Inventario general |
| Leer briefing / README del propietario | `projects/<proyecto>/README.md` | `proposito`, `alcance`, `referencias` |
| Identificar el manifest de dependencias | `package.json`, `pom.xml`, `requirements.txt`, `go.mod`, `Cargo.toml` | `metadata`, stack, versiones |
| Identificar el manifest de build/infra | `Dockerfile`, `docker-compose.yml`, `angular.json`, `next.config.js`, `vite.config.ts`, `build.gradle` | Infraestructura y empaquetado |

Alimenta: `metadata`, `proposito`, `alcance`, `referencias`.

---

## 1. Arquitectura del sistema

| Acción | Pista en el código | Clave alimentada |
|---|---|---|
| Detectar puntos de entrada | `main.ts`, `index.ts`, `App.tsx`, `wsgi.py`, `manage.py`, `main.py`, `Program.cs` | `descripcion_del_sistema` |
| Detectar enrutamiento | `routes.ts`, `app.routes.ts`, `pages/`, `urls.py`, `controllers/`, `routers/` | `descripcion_del_sistema` |
| Detectar componentes y módulos | `src/components/**`, `src/app/**`, `src/modules/**`, `src/views/**` | `division_sistema.capas` |
| Detectar servicios y lógica de negocio | `src/services/**`, `src/api/**`, `lib/`, `use_cases/`, `domain/` | `funcionalidades_sistema` |
| Detectar persistencia | Configuración de BD, `prisma/`, `migrations/`, `repositories/`, uso de `localStorage` o `IndexedDB` | `situacion_actual`, `resguardo_documentos_por_tipo_documental` |
| Detectar integraciones externas | URLs HTTP en el código, claves de API, SDKs de terceros | `referencias`, `principales_conceptos_relaciones` |

---

## 2. Modelo de dominio

| Acción | Pista en el código | Clave |
|---|---|---|
| Buscar interfaces, tipos y DTOs | `interface `, `type `, `class `, `dataclass`, `model.py`, `entity/` | `principales_conceptos_relaciones.entidades` |
| Inferir relaciones entre entidades | Claves foráneas, referencias entre tipos, asociaciones en ORM | `principales_conceptos_relaciones.relaciones` |
| Identificar catálogos de dominio | Archivos JSON/YAML/CSV estáticos en `public/`, `assets/`, `fixtures/`, `seeds/` | `principales_conceptos_relaciones`, `referencias` |

---

## 3. Funcionalidades extremo a extremo

Para cada funcionalidad identificada, construye un objeto con la siguiente estructura:

```json
{
  "id": "F-XX",
  "nombre": "Nombre orientado al negocio (capacidad, no componente técnico)",
  "descripcion": "Qué valor aporta al usuario u organización; qué hace el sistema; qué entra y qué sale.",
  "componentes": ["ComponenteA", "ServicioB"],
  "fuente": "ruta/relativa/al/archivo.ts"
}
```

**Heurística:** una funcionalidad equivale a una **capacidad de negocio** que el sistema otorga
al usuario o a la organización. El nombre y la descripción deben redactarse desde la perspectiva
del valor; los componentes técnicos actúan como soporte, no como protagonistas.

Busca en: handlers de rutas, controladores, casos de uso (`use_cases/`), y componentes con
lógica de interfaz de usuario.

**Ejemplo incorrecto:**
> "El componente `DashboardComponent` llama a `DataService` para cargar `datos.json` y renderiza la vista."

**Ejemplo correcto:**
> "Permite al usuario consultar y registrar el avance de sus tareas diarias; el sistema
> presenta el plan correspondiente a la fecha en curso y actualiza el historial de forma automática."

---

## 4. Roles del sistema

- Si existe autenticación → leer `auth/`, `guards/`, `middleware/`, `permissions/`, `roles/` para extraer roles.
- Si no existe autenticación → suele haber un único rol "Usuario". Documentarlo así y justificar la ausencia de autenticación.

Alimenta: `definicion_roles`.

---

## 5. Caso de uso principal

Selecciona la funcionalidad más representativa del sistema (la que destaca el README o la que
concentra mayor flujo de código). Reconstruye:

- **Actor:** quién inicia la acción.
- **Precondición:** estado requerido antes de que el flujo comience (sesión activa, datos cargados, conexión disponible).
- **Flujo básico:** pasos numerados citando los métodos y servicios reales involucrados.
- **Postcondición:** estado final del sistema tras la ejecución exitosa (registro en BD, actualización en `localStorage`, archivo generado, etc.).

Alimenta: `caso_uso_principal`.

---

## 6. Requisitos funcionales y no funcionales

Construye una tabla de **5 a 12 requisitos**. Reglas de redacción:

- **RF (Funcional):** capacidad que el sistema debe proveer al negocio o al usuario. Verbo en infinitivo desde la perspectiva del valor ("Registrar el cumplimiento diario de…", "Permitir la exportación del historial…"). Trazable a un componente o servicio del código.
- **RNF (No funcional):** restricción transversal (disponibilidad, rendimiento, seguridad, compatibilidad, mantenibilidad, observabilidad). Redactar también en términos de impacto al negocio cuando sea posible.
- Cada requisito con prioridad `Alta | Media | Baja`.
- El enunciado de cada requisito describe una **obligación del sistema**, no una descripción de implementación.

Alimenta: `tabla_requisitos`.

---

## 7. Annexo 1 — Requisitos estructurales Santander (SO/FI/DI/US)

Es **obligatorio** evaluar los 7 requisitos estructurales listados en `tags-catalog.md` (SO001..US001).
Para cada uno:

- `aplica_al_sistema`: `true` o `false`.
- `justificacion`: una frase que cite el componente, archivo o módulo que justifica la decisión.

Guía de evaluación:

| Requisito | Cuándo aplica | Cuándo no aplica |
|---|---|---|
| SO001 — Multiempresa | El código tiene concepto de organización, workspace o tenant | Aplicaciones personales o B2C sin segmentación |
| SO002 — Multidivisa | El sistema gestiona valores monetarios | Sistemas que no manejan importes |
| SO003 — Independencia de canal | La lógica de negocio está separada de la capa de presentación | Aplicaciones monolíticas con lógica en la vista |
| SO004 — Multilengua | El sistema tiene internacionalización (i18n) | Aplicaciones de idioma único sin planes de expansión |
| FI001 — Disponibilidad 24h | Arquitectura stateless, réplicas o SLA definido | Uso personal sin SLA |
| DI001 — Integración corporativa | El sistema se conecta a sistemas del banco | Sistemas aislados o de uso interno no bancario |
| US001 — Compatibilidad de navegadores | Aplicación web accesible desde varios navegadores | Aplicaciones nativas, CLI o de escritorio |

---

## 8. Vocabulario común y acrónimos

Incluir entre 8 y 20 términos. Combinar:

- Acrónimos técnicos del proyecto (SPA, PWA, JWT, API, SLA, CI/CD, ORM…).
- Términos del dominio del negocio específico al sistema documentado.
- Acrónimos corporativos relevantes (DDS, TI, NF, RF…).

Alimenta: `vocabulario[]`.

---

## 9. Situación actual

Describe el estado real del repositorio **antes** de este DDS:

- Fase del proyecto: MVP, producción, prototipo, piloto, etc.
- Pipelines de CI/CD: ¿definidos? ¿en qué plataforma?
- Infraestructura de alojamiento elegida o en uso.
- Cobertura de pruebas automatizadas (si existe).
- Riesgos conocidos: punto único de fallo, dependencias sin soporte, lock-in tecnológico, deuda técnica visible.

Alimenta: `situacion_actual`.

---

## 10. Introducción a la sección de análisis (sección 3)

Redacta un párrafo introductorio breve (3-5 oraciones) que contextualice la sección 3
del DDS: qué contiene, qué metodología se siguió para el análisis y cuál es el alcance
de los requisitos identificados.

Alimenta: `analisis_y_definicion_de_requisitos`.

---

## 11. Pendientes

Recoge en `pendientes[]`:

- TODO / FIXME / XXX detectados en el código que afectan a la funcionalidad documentada.
- Diagramas opcionales no generados (diagramas de flujo, diagramas de arquitectura).
- Pruebas automatizadas faltantes.
- Documentación auxiliar pendiente (manuales de usuario, guías de despliegue).

Alimenta: `pendientes[]`.

---

## 12. Evidencia trazable

Mantén un mapa `evidencia` en el JSON con un alias por archivo clave:

```json
"evidencia": {
  "config_app":  "projects/<proyecto>/src/app/app.config.ts",
  "rutas":       "projects/<proyecto>/src/app/app.routes.ts",
  "servicio_X":  "projects/<proyecto>/src/services/x.service.ts",
  "manifest":    "projects/<proyecto>/package.json"
}
```

Esto permite auditar el DDS sin necesidad de releer todo el repositorio.

---

## Manejo de lagunas — generación de `cuestionario.md`

Para cada campo del DDS sin evidencia verificable en el código:

1. Asignar el valor `"PENDIENTE_CUESTIONARIO"` en `agente.json`.
2. Añadir una entrada `Q-<NN>` en `data/<proyecto>/cuestionario.md` siguiendo `questionnaire-format.md`.
3. Añadir el resumen también a `pendientes[]`.

**Reglas de calidad de las preguntas:**

- Una sola idea por pregunta, formulación concreta.
- Indicar el `Tipo esperado` para que el usuario conozca el formato de respuesta.
- Mencionar en `Por qué pregunto` los archivos que se revisaron y descartaron.
- Marcar `Bloqueante: Sí` únicamente si el campo es indispensable para la entrega del DDS.
- Agrupar preguntas relacionadas en bloques contiguos por sección DDS.

**Antes de cerrar la Fase 2**, si se generó cuestionario, comunicar al usuario:

> Se han identificado N lagunas que no pueden cubrirse desde el código.
> Las preguntas se encuentran en `data/<proyecto>/cuestionario.md`.
> Una vez respondidas, invocar `/answer-questionnaire <proyecto>` para incorporarlas.
> Es posible renderizar con sentinels visibles, pero el documento resultante no debe entregarse en ese estado.

---

## Validación antes de pasar a la Fase 3

1. Todas las claves del esqueleto de `tags-catalog.md` están presentes en el JSON.
2. Ningún campo de tipo `string` contiene `"..."` ni placeholders sin completar.
3. `Get-Content 'data/<proyecto>/agente.json' -Raw | ConvertFrom-Json | Out-Null; Write-Host 'OK'` retorna sin error.
4. El array `pendientes[]` recoge todo lo que no pudo cubrirse con evidencia del código.
5. Si hay sentinels `PENDIENTE_CUESTIONARIO`, existe `data/<proyecto>/cuestionario.md` con al menos una pregunta por cada sentinel.
