.PHONY: help generate run test build verify package clean up down restart logs ps stop rm \
        docker-build docker-rebuild docker-pull db-shell app-shell \
        health swagger openapi wait

help:
	@echo "Comandos disponíveis:"
	@echo "  make generate       - Gera fontes OpenAPI"
	@echo "  make run            - Executa app local (profile dsv)"
	@echo "  make test           - Executa testes"
	@echo "  make build          - Build completo com verify"
	@echo "  make package        - Empacota jar sem testes"
	@echo "  make clean          - Limpa artefatos Maven"
	@echo "  make up             - Sobe stack Docker Compose"
	@echo "  make down           - Derruba stack Docker Compose"
	@echo "  make restart        - Reinicia stack"
	@echo "  make logs           - Logs da stack"
	@echo "  make ps             - Status dos containers"
	@echo "  make health         - Testa actuator health"
	@echo "  make swagger        - Testa Swagger UI"
	@echo "  make openapi        - Testa OpenAPI docs"
	@echo "  make db-shell       - Shell no Postgres"
	@echo "  make app-shell      - Shell no container da API"

generate:
	./mvnw -q -DskipTests generate-sources

run:
	./mvnw -P dsv spring-boot:run

test:
	./mvnw test

build:
	./mvnw clean verify

verify:
	./mvnw clean verify

package:
	./mvnw -DskipTests clean package

clean:
	./mvnw clean

up:
	docker compose up -d --build

down:
	docker compose down

restart: down up

logs:
	docker compose logs -f --tail=200

ps:
	docker compose ps

stop:
	docker compose stop

rm:
	docker compose rm -f

docker-build:
	docker compose build

docker-rebuild:
	docker compose build --no-cache

docker-pull:
	docker compose pull

db-shell:
	docker compose exec postgres-acervo-digital psql -U $${DB_USERNAME:-vfa_app} -d $${DB_NAME:-db-acervo-digital}

app-shell:
	docker compose exec api-acervo-digital sh

health:
	curl -fsS http://localhost:$${API_PORT_EXTERNAL:-18019}/api/v1/actuator/health || (echo "healthcheck falhou" && exit 1)

swagger:
	curl -I -fsS http://localhost:$${API_PORT_EXTERNAL:-18019}/api/v1/swagger-ui/index.html >/dev/null || (echo "swagger indisponível" && exit 1)

openapi:
	curl -fsS http://localhost:$${API_PORT_EXTERNAL:-18019}/api/v1/v3/api-docs >/dev/null || (echo "openapi indisponível" && exit 1)

wait:
	@echo "Aguardando aplicação ficar saudável..."
	@for i in $$(seq 1 60); do \
	  if curl -fsS http://localhost:$${API_PORT_EXTERNAL:-18019}/api/v1/actuator/health >/dev/null 2>&1; then \
	    echo "Aplicação saudável"; exit 0; \
	  fi; \
	  sleep 2; \
	done; \
	echo "Timeout aguardando health"; exit 1
