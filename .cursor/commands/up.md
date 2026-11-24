---
name: up
description: Start the local PostgreSQL container
---

# Start PostgreSQL

From the repository root, run:

```bash
docker compose up -d --build
```

`POSTGRES_PASSWORD` must already be set in the environment or in an untracked `.env` file. Do not print the password. Do not write it into a tracked file.

The database name and user are `teste_ia`. The host port is `5432`. Report whether the container is running. Leave it running.
