.PHONY: run stop test build clean infra-up infra-down smoke

run:
	docker compose up --build -d

stop:
	docker compose down

test:
	./gradlew test

build:
	./gradlew build

clean:
	./gradlew clean && docker compose down -v

infra-up:
	docker compose -f docker-compose.infra.yml up -d

infra-down:
	docker compose -f docker-compose.infra.yml down

smoke:
	./scripts/smoke-test.sh
