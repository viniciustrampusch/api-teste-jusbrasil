---
name: run-prod
description: Build the jar and run it with the prod profile
---

# Run in production mode

1. Build the jar with `./gradlew bootJar`.
2. Run it with a Java 25 runtime. The system `java` command may be older and will fail on a Java 25 class file. Use the toolchain JDK under `~/.gradle/jdks` when that is the available Java 25.

```bash
"$JAVA_25" -jar build/libs/engineering-baseline-0.1.0.jar --spring.profiles.active=prod
```

3. Request `http://localhost:8080/actuator/health`. The prod profile must not include health details.
4. Report the command, the Java version used, and the response body. Stop the process when the check is finished unless the user wants it left running.
