.PHONY: copy-env test verify run stop logs clean

copy-env:
	cp .env.example .env

test:
	docker compose run --rm test mvn -B test

verify:
	docker compose run --rm test mvn -B verify

run:
	docker compose up --build

stop:
	docker compose down

logs:
	docker compose logs -f app

clean:
	docker compose down -v
