# Build & Test Commands

## Build / package

```bash
mvn clean install              # build all modules + run unit tests + JaCoCo report
mvn clean package -DskipTests  # build without tests
```

## Tests

- **Unit tests** (`*Test.java`) → Surefire via `mvn test`
- **Integration tests** (`*IT.java`) → Failsafe via `mvn verify -Pintegration-tests`
- **Coverage gate** (≥80% LINE + BRANCH): `mvn verify -Pcoverage`
- Parallel JUnit5 enabled: `parallel.enabled=true`, dynamic factor 1
- `testFailureIgnore=true` configured — DON'T rely on exit code, parse reports
- AspectJ weaver loaded for Allure `@Step` instrumentation

## Allure reports

```bash
mvn allure:serve   # results dir: target/allure-results
```

## Static analysis

- **Spotless** auto-applies `palantirJavaFormat` on `compile` phase (Java + Markdown via flexmark)
- **PMD** check fails build on violations (`configs/pmd-ruleset.xml`)
- **JaCoCo** writes to `${argLine}` (empty by default — JaCoCo overrides)

## Release

```bash
mvn clean deploy -Prelease     # GPG sign + central publishing (autoPublish=true)
```

## Important argLine

Surefire/Failsafe argLine references aspectjweaver from `${settings.localRepository}` — needs local mvn repo populated.
