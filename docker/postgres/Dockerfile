# Local PostgreSQL for development.
# Start and stop it from the repository root with `docker compose up` and
# `docker compose down`. The password is supplied at runtime and is not stored
# in the image.
FROM postgres:17

ENV POSTGRES_DB=teste_ia \
    POSTGRES_USER=teste_ia

EXPOSE 5432

HEALTHCHECK --interval=10s --timeout=5s --start-period=20s --retries=5 \
    CMD pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB"
