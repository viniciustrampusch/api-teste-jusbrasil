---
name: down
description: Stop the local PostgreSQL container
---

# Stop PostgreSQL

From the repository root, run:

```bash
docker compose down
```

This stops and removes the container and the network. The named volume `teste-ia-postgres` stays, so the data remains. Do not pass `-v` unless the user asks to delete the data. Report the command result.
