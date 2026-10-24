# Conduit: Java submission (Spring Boot 4, virtual threads)

**Front: direct.** Embedded Tomcat serves clients itself on `0.0.0.0:80` — no Nginx, so the ~11% CPU Nginx
would use stays available to the JVM and Postgres.

## Stack

| Piece | Version |
|---|---|
| Language | Java 27 (Eclipse Temurin, checksum-verified in `install.sh`) |
| Framework | Spring Boot 4.1.1 (`spring-boot-starter-webmvc`, `spring-boot-starter-jdbc`) |
| Web server | Embedded Tomcat 11.0.24, one virtual thread per request |
| JDBC driver | PostgreSQL JDBC (pgjdbc) 42.7.14 over HikariCP 7.0.2 |
| Security | spring-security-crypto 7.1.1, `BCryptPasswordEncoder(10)` only |
| JWT | auth0 `java-jwt` 4.6.1, HS256 |
| Build | Maven 3.9.16 via the checked-in wrapper (`./mvnw`) |

Based on the repo's `examples/java` server, with its `Front: nginx` default switched to direct.

## Scripts

- `install.sh` — root, once: installs Temurin JDK 27+35 to `/opt/jdk`.
- `build.sh` — normal user: `./mvnw -q -B -DskipTests package` → `target/conduit.jar`.
- `start.sh` — foreground: `exec java -jar target/conduit.jar`. Config from `DATABASE_URL`, `JWT_SECRET`,
  `HOST`, `PORT` per the runtime contract.

## Optimizations (baseline: example code, verbatim)

None yet. This submission starts as the unmodified example, which already does:

- virtual threads (ForkJoinPool of carrier threads sized to the core count; a request blocked on JDBC unmounts),
- HikariCP pool of 10 connections per core, connection timeout 5 s,
- Tomcat sized for direct mode: keep-alive 65 s, unlimited keep-alive requests, 20k connections,
- explicit JDBC parameter types so `COALESCE($n, col)` never triggers an extra describe round trip,
- Jackson-tree body parsing and a catch-all error shape per SPEC.
