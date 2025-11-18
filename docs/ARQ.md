# Arquitectura propuesta para proyecto Java + Spring Boot

## Resumen

Documento de referencia que describe una arquitectura limpia y práctica para desarrollar un proyecto Java con Spring Boot. Cubre estructura de paquetes, capas, dependencias recomendadas, estrategia de datos, pruebas, despliegue y observabilidad.

## Principios de diseño

- Separación de responsabilidades (CAPAS): Controller → Service → Repository.
- Código orientado a dominio (entidades limpias), DTOs para transporte.
- Validación en el borde (controllers/DTOs) y validación en backend (servicios).
- API-first: contratos estables, versionado de API y pruebas contractuales.
- Automatización: pruebas, CI/CD, infra como código, migraciones de esquema.

## Estructura de paquetes (sugerida)

Usar un paquete base consistente con el proyecto, por ejemplo `com.proyecto.demo`.

- `com.proyecto.demo` (root)
  - `config` : configuraciones Spring, beans, seguridad, CORS.
  - `web` (o `api`) : controladores REST, DTOs, mappers.
  - `service` : lógica de negocio, casos de uso, servicios transaccionales.
  - `repository` : interfaces Spring Data JPA / Repositorios.
  - `domain` : entidades JPA / modelos de dominio.
  - `dto` : objetos de transferencia (si no se colocan en `web`).
  - `exception` : clases de excepción y manejadores globales (ControllerAdvice).
  - `util` : utilidades (por ejemplo `SlugUtil`, `Slugify`).
  - `migration` : scripts y configuraciones para Flyway/Liquibase.

## Dependencias recomendadas

- Spring Boot Starter Web
- Spring Boot Starter Data JPA
- Driver JDBC (PostgreSQL recomendado)
- Flyway o Liquibase (migraciones)
- Spring Boot Starter Security (si aplica)
- Spring Boot Starter Validation (Hibernate Validator)
- Lombok (opcional, abrevia boilerplate)
- MapStruct (o ModelMapper) para mapeo DTO↔Entidad
- Micrometer + Prometheus registry (métricas)
- Spring Boot Actuator (salud, metrics)
- Logback + SLF4J
- JUnit 5 + Mockito para pruebas unitarias

Mantener `pom.xml` o `build.gradle` organizado por scopes (compile, test).

## Diseño de persistencia

- Usar Spring Data JPA para repositorios sencillos y consultas básicas.
- Definir índices en columnas críticas (por ejemplo `slug` con UNIQUE index).
- Usar migraciones (Flyway) para versionar el esquema de BD.
- Estrategia de transacciones: servicios marcados con `@Transactional` cuando modifiquen datos.
- Evitar lógica de negocio en entidades o repositorios; ponerla en servicios.

## API y controladores

- Seguir convenciones REST: recursos plurales (`/api/posts`), usar HTTP verbs adecuadamente.
- Manejar errores con un `@ControllerAdvice` que normalice respuestas de error.
- Versionado de API: prefijo en ruta (`/api/v1/...`) o headers.

## Seguridad

- Autenticación/Autorización: OAuth2 / JWT según necesidades.
- Validar y sanear inputs en los controladores y servicios.
- Proteger endpoints administrativos y de publicación.

## Validación y manejo de errores

- Validaciones con `@Valid` y `@Validated` (JSR-380).
- Mensajes de error amigables y códigos HTTP adecuados (400, 401, 403, 404, 409, 500).
- Manejar conflicto de recursos (`409 Conflict`) cuando, por ejemplo, un `slug` ya existe.

## Caching

- Caching (Redis o cache local) para endpoints de lectura intensiva si el tráfico lo exige.
- Cachear resultados de consultas frecuentes (por ejemplo, listado de posts populares).

## Observabilidad

- Logs estructurados con contexto (requestId/correlationId).
- Métricas: Micrometer → Prometheus.
- Health checks y endpoints de Actuator.
- Trazas distribuidas: OpenTelemetry / Jaeger si servicio es distribuido.

## Testing

- Unit tests: JUnit 5 + Mockito para servicios y utilidades.
- Integration tests: Spring Boot Test con perfil `test`, H2 o contenedores (Testcontainers) para DB real.
- Contract/API tests: usar `spring-restdocs` o `pact` si hay consumidores externos.
- End-to-end: opcional según alcance.

## CI/CD y calidad

- Pipelines:
  - Build → Test → Lint/Static Analysis → Build Artifact → Publish Docker image → Deploy to staging.
- Tools: GitHub Actions / GitLab CI / Jenkins.
- Quality gates: SonarQube (opcional), cobertura mínima de tests.

## Contenedores y despliegue

- Dockerfile lean basado en `eclipse-temurin` o `corretto` JRE/JDK.
- Multi-stage build: build + runtime image.
- Orquestación: Kubernetes + manifest/Helm charts o plataformas PaaS (Heroku, Cloud Run).
- Variables de entorno para configuración (no almacenar secrets en imágenes; usar Vault/Kubernetes Secrets).

## Migraciones y retrocompatibilidad

- Migraciones con Flyway: cada cambio de esquema en un script versionado.
- Estrategia de despliegue que soporte cambios no rompientes en DB (backwards compatible).

## Performance y escalado

- Emplear paginación en endpoints que devuelvan listas.
- Usar índices adecuados en la BD; medir consultas costosas.
- Escalar horizontalmente la app y la capa de BD según necesidad.

## Ejemplo: paquete y clase para `slugify`

Archivo sugerido: `com.proyecto.demo.util.SlugUtil`

- Función: normalizar título → slug ASCII minúscula → truncar (máx 100) → eliminar caracteres inválidos → evitar guiones dobles.
- Considerar transliteración (ej. `ñ` → `n`) y reemplazo de acentos.

Pseudocódigo:

```
String slugify(String title) {
  String s = transliterate(title);
  s = s.toLowerCase(Locale.ROOT);
  s = s.replaceAll("[^a-z0-9\\s-]", "");
  s = s.replaceAll("[\\s]+", "-");
  s = s.replaceAll("-+", "-");
  s = trimHyphens(s);
  return truncateCleanly(s, 100);
}
```

## Recomendaciones finales

- Mantener el código modular y con responsabilidades claras.
- Escribir pruebas al mismo tiempo que la funcionalidad.
- Automatizar migraciones y despliegues.
- Empezar con perfiles `dev` y `prod` bien definidos.

---
Archivo: `docs/ARQ.md` — última actualización: 2025-11-18
