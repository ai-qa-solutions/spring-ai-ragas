# Java Code Style — spring-ai-ragas

Source of truth: `.claude/refs/java-patterns.md` + `.claude/refs/java-testing.md`. Read them when starting non-trivial work.

## Hard rules

1. **No-Nest** — max 1 nesting level per method. Extract to private helpers / use streams
2. **Atomic methods** — one method = one thing. Name = WHAT it does
3. **Fail-fast** — `org.springframework.util.Assert.*` at method entry. NO manual `if/throw`
   - `Assert.hasText`, `Assert.notNull`, `Assert.notEmpty`, `Assert.isTrue`, `Assert.state`
4. **No `var`** — explicit types everywhere
5. **`final` everywhere** — variables AND method parameters. Compiler-enforced immutability
6. **Lombok** — `@Data`, `@Builder`, `@Slf4j`, `@RequiredArgsConstructor`, `@Value`. No manual boilerplate
7. **Comments mandatory** for: classes, public methods, **ALL fields (including private)**. Russian Javadoc OK. Concise — no essays
8. **No mutable static state** — static logger OK (Lombok `@Slf4j`); static counters/caches NOT OK
9. **Named constants** — no magic numbers/strings. `private static final` constants
10. **Try-with-resources** for all I/O

## Java 17+ features (project on Java 17)

- **Records** for DTOs / value objects (replaces Lombok `@Value`). Use compact constructor for fail-fast validation
- **Pattern matching for `instanceof`** — no manual cast
- **Switch expressions** with `->` arrows
- **Text blocks** for SQL/JSON/YAML
- Java 21 features (sealed switch patterns, virtual threads, sequenced collections) — DO NOT use; project on Java 17

## Error handling (Spring Boot way)

- NO Result/Either/sealed pattern. Throw exceptions
- Custom exceptions with `@ResponseStatus(HttpStatus.X)`
- `@RestControllerAdvice` for global handling
- `Assert.xxx()` IllegalArgumentException → 400 via global handler
- Service: just throw `NotFoundException` / `ConflictException`
- Controller: clean, no try/catch

## ConfigurationProperties

For `@ConfigurationProperties` classes, Javadoc MUST include full YAML example with all properties.

## Tooling

- Spotless `palantirJavaFormat` enforces formatting (auto-applied on compile)
- PMD `configs/pmd-ruleset.xml` — fails build on violation

