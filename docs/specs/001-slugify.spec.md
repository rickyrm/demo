---
description: 'Spec 001 - Generación y validación de slugs SEO-friendly a partir de títulos.'
applyTo: '/docs/specs/001-slugify.spec.md'
---

# 001-slugify-friendly-slugs.spec.md

## 1. 👔 Problem Specification

Contexto: según el `PRD` el blog necesita generar slugs amigables a partir de los títulos, validar su longitud y formato siguiendo buenas prácticas SEO, y asegurar unicidad y comportamiento correcto cuando se editan slugs de entradas publicadas.

### Historias de usuario

### Historia 1 — Generación automática de slug
- **Como** autor
- **Quiero** que al crear una entrada se genere automáticamente un slug a partir del título
- **Para que** la URL sea legible y optimizada para SEO sin intervención manual.

### Historia 2 — Edición segura de slug y redirección
- **Como** editor
- **Quiero** poder editar manualmente el slug cuando sea necesario
- **Para que** pueda afinar la URL; además, si el post ya fue publicado, la antigua URL redirija (301) a la nueva.

### Historia 3 — Validaciones SEO en título y meta description
- **Como** autor
- **Quiero** recibir avisos cuando el título o la meta description excedan los límites recomendados
- **Para que** los contenidos cumplan estándares SEO (títulos ≈ 50–60 chars, meta ≈ 150–160 chars).

### Historia 4 — Comprobación de unicidad y resolución de colisiones
- **Como** sistema
- **Quiero** asegurar que los slugs sean únicos en la base de datos
- **Para que** no existan URLs duplicadas; en caso de colisión proponer sufijo incremental (`-2`, `-3`, ...).

## 2. 🧑‍💻 Solution Design

### Resumen de la solución

- Implementar un utilitario `slugify` en backend que:
  - Transliterará caracteres no ASCII (á, ñ, ü, etc.).
  - Convertirá a minúsculas.
  - Reemplazará espacios y separadores por `-`.
  - Eliminará caracteres inválidos, evitará `--` y trim de `-` inicial/final.
  - Truncará a 100 caracteres preferiblemente sin cortar palabras.
- Comprobará unicidad contra la tabla `posts.slug`; en caso de conflicto añadirá sufijo incremental.
- Permitir edición manual del slug en el API con las mismas validaciones.
- Si se modifica el slug de una entrada publicada, crear registro de redirección 301 y guardar historial.

### Data models

- `Post` (persistido):
  - `id: UUID`
  - `title: String`
  - `slug: String` (unique, indexed)
  - `metaDescription: String`
  - `status: enum {DRAFT, PUBLISHED}`
  - `publishedAt: Instant`

- `SlugRedirect` (persistido):
  - `id`
  - `oldSlug`
  - `newSlug`
  - `postId`
  - `createdAt`

### API Endpoints

- `POST /api/v1/posts` — crea post
  - Request: `{ title, content, metaDescription, optional slug }`
  - Behavior: si no viene `slug` llamar a `slugify(title)` y resolver unicidad; retornar recurso creado con `201`.

- `PUT /api/v1/posts/{id}` — actualiza post
  - Request: `{ title?, metaDescription?, slug? }`
  - Behavior: si `slug` cambia y `status==PUBLISHED` crear `SlugRedirect` y marcar para 301.

- `GET /api/v1/posts/slug-available?slug=...` — chequear disponibilidad

### Componentes de software

- `com.proyecto.demo.util.SlugUtil` — implementa `slugify(String): String`.
- `com.proyecto.demo.service.PostService` — lógica que crea/comprueba slugs y maneja redirecciones.
- `com.proyecto.demo.repository.PostRepository` — consultas JPA, índice en `slug`.
- `com.proyecto.demo.web.PostController` — endpoints REST, validación de entrada con `@Valid`.

### Reglas de negocio y edge cases

- Si `slugify(title)` resulta vacío (por ejemplo título con solo caracteres eliminados), usar `post-{id}` o una forma fallback.
- Al truncar, preferir truncar en el último `-` antes del límite para evitar cortar palabras.
- Sufijo incremental: comprobar `slug`, `slug-2`, `slug-3`, ... hasta encontrar disponible; operación atómica o retry para evitar races (usar UNIQUE constraint y retry en transacción si falla).

### Seguridad y validación

- Validar en backend límites de longitud del título y `metaDescription` (título recomendado ≤ 60, meta ≤ 160) y devolver advertencias o `400` en caso de restricciones estrictas.
- Sanitizar inputs para prevenir inyección en campos que luego se incluyen en HTML.

### Observabilidad

- Logs al crear slugs y cuando hay colisiones.
- Métrica: contador `slug_collisions_total` para monitorizar frecuencia de colisiones.

## 3. 🧑‍⚖️ Acceptance Criteria (EARS)

- Historia 1 — Generación automática de slug:
  - SHALL: Cuando un autor crea una entrada sin `slug` proporcionado
  - WHEN: Se invoca `POST /api/v1/posts` con `title` no vacío
  - THEN: El sistema SHALL generar un `slug` válido siguiendo las reglas (ASCII minúsculas, `-` como separador, longitud ≤ 100) y persistirlo en `posts.slug`.

- Historia 2 — Edición segura y redirección:
  - SHALL: Cuando un editor actualiza el `slug` de una entrada publicada
  - IF: `PUT /api/v1/posts/{id}` cambia el `slug` y `status == PUBLISHED`
  - THEN: El sistema SHALL crear un registro `SlugRedirect(oldSlug, newSlug)` y responder que la redirección 301 será servida para el `oldSlug`.

- Historia 3 — Validaciones SEO:
  - SHALL: Cuando un autor envía un `title` > 60 caracteres o `metaDescription` > 160 caracteres
  - WHEN: En `POST` o `PUT` del recurso
  - THEN: El sistema SHALL devolver advertencias en el response (campo `warnings`) indicando qué excede el umbral; NO bloquear por defecto (salvo política distinta solicitada).

- Historia 4 — Unicidad y resolución de colisiones:
  - SHALL: Cuando `slugify(title)` produce un `slug` ya existente
  - IF: `posts.slug` ya contiene ese valor
  - THEN: El sistema SHALL proponer y persistir un `slug` con sufijo incremental (`-2`, `-3`...) garantizando unicidad; en caso de race condition, la operación SHALL reintentar hasta N intentos y fallar con `409 Conflict` si persiste.

## Deliverables

- `docs/specs/001-slugify.spec.md` (este archivo).
- Especificación técnica para `SlugUtil` y `PostService`.
- Lista de pruebas automáticas: unit tests para `SlugUtil`, integración para colisiones y redirecciones.

---
Última actualización: 2025-11-18
