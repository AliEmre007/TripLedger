.PHONY: copy-env test verify run smoke stop logs clean

copy-env:
	cp .env.example .env

test:
	docker compose run --rm test mvn -B test

verify:
	docker compose run --rm test mvn -B verify

run:
	docker compose up --build

smoke:
	@port=$${APP_PORT:-18080}; \
	curl -fsS "http://localhost:$${port}/api/v1/health/live" >/dev/null; \
	curl -fsS "http://localhost:$${port}/api/v1/health/ready" >/dev/null; \
	curl -fsS "http://localhost:$${port}/actuator/health/liveness" >/dev/null; \
	curl -fsS "http://localhost:$${port}/actuator/health/readiness" >/dev/null; \
	echo "Smoke checks passed on port $${port}"

stop:
	docker compose down

logs:
	docker compose logs -f app

clean:
	docker compose down -v
