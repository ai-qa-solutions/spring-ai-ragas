# Testing Standards — spring-ai-ragas

Source: `.claude/refs/java-testing.md`. Read fully before writing tests.

## Philosophy

- **Real integration tests** = main bucket (HTTP/Kafka/JDBC via Testcontainers + Podman). Catch real bugs, stable
- **Unit tests with mocks** = top up coverage to 80/80 JaCoCo, edge cases only
- **NEVER** write performance benchmarks or throughput tests in suite — those belong in JMeter/Gatling

## Naming

- Method format: `method_condition_expectedResult`
  - `createOrder_withValidItems_returnsOrderWithCorrectTotal`
- File suffixes:
  - `*Test.java` → Surefire (`mvn test`) — unit
  - `*IT.java` → Failsafe (`mvn verify -Pintegration-tests`) — integration

## Structure

- **Given-When-Then** comments mandatory: `// given`, `// when`, `// then`
- **AssertJ** assertions only (not JUnit)
- **`@Nested` classes** to group by method under test
- **TestDataBuilders** — reusable test data factory class (final, private ctor)

## Allure

- `@Epic`, `@Feature`, `@Story`, `@Severity`, `@Description` on classes/tests
- `@Step("...")` on private helper methods for readable reports
- AspectJ weaver loads automatically via Surefire/Failsafe argLine

## Scenario priority

1. Positive (happy path) — first
2. Critical negatives — 404, 400, 409, 401/403
3. Edge cases — unit tests with Mockito (concurrent updates, retry, partial states)

## Integration tests

- `BaseIntegrationTest` abstract class with `@Testcontainers` + `@ServiceConnection`
- `@SpringBootTest(webEnvironment = RANDOM_PORT)`, `@ActiveProfiles("test")`
- Cleanup data in `@BeforeEach`
- HTTP: `TestRestTemplate`
- Kafka: real broker via `KafkaContainer`
- JDBC: real PostgreSQL via `PostgreSQLContainer`
- External APIs: WireMock (`@WireMockTest`)
- Async: `Awaitility` (`await().atMost(...).untilAsserted(...)`)

## Podman setup (replaces Docker)

- macOS: `DOCKER_HOST=unix://$(podman machine inspect --format '{{.ConnectionInfo.PodmanSocket.Path}}')` + `TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock`
- Linux: `DOCKER_HOST=unix://${XDG_RUNTIME_DIR}/podman/podman.sock`
- Rootless: `TESTCONTAINERS_RYUK_DISABLED=true` (Ryuk doesn't work rootless)

## Coverage

- 80% LINE + 80% BRANCH via JaCoCo (`mvn verify -Pcoverage`)
- Mockito generated classes excluded

## E2E (if frontend exists — not currently in this repo)

- Selenide + `BrowserWebDriverContainer`
- ARM64 Mac: `seleniarm/standalone-chromium`
- Podman: `host.containers.internal` (NOT localhost) inside containers
- AllureSelenide listener for screenshots

