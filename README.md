# api-acervo-digital

Microsserviço do domínio acervo-digital da plataforma Via Fluvial.

## Stack

- Java 21
- Spring Boot 3.5.x
- PostgreSQL + Flyway
- OpenAPI 3.1 (API-first)

## Configuração

Parâmetros principais por ambiente:

- `SPRING_PROFILES_ACTIVE`: `dsv|hml|prd`
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_SCHEMA`, `DB_USERNAME`, `DB_PASSWORD`
- `SECURITY_MODE`: `dev|oauth2`
- `ACERVO_QUARANTINE_BUCKET`, `ACERVO_PUBLIC_MEDIA_BUCKET`, `ACERVO_PRIVATE_DOCUMENTS_BUCKET`, `ACERVO_CDN_BASE_URL`

## Execução local

```bash
./mvnw -P dsv spring-boot:run
```

## Build e testes

```bash
./mvnw clean verify
```

## API-first

Contrato OpenAPI oficial:

- `src/main/resources/openapi/openapi.yaml`
- `src/main/resources/static/openapi/openapi.yaml`

Gerar fontes OpenAPI:

```bash
./mvnw -DskipTests generate-sources
```

## Endpoints operacionais

Com a aplicação rodando em `localhost:18019` (via Docker Compose):

- Health: `GET /api/v1/actuator/health`
- Swagger UI: `GET /api/v1/swagger-ui/index.html`
- OpenAPI runtime: `GET /api/v1/v3/api-docs`
- OpenAPI estático: `GET /api/v1/openapi/openapi.yaml`

## Docker

```bash
make up
make wait
make health
```

## Flyway

- Estrutura: `src/main/resources/db/migration` (`V001..V099`)
- Seeds dsv: `src/main/resources/db/seed/dsv` (`V101..V199`)
- `hml/prd` executam apenas migrations estruturais.
