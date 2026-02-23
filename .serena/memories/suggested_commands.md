# Suggested Commands

## Build

```bash
mvn clean install                    # Full build
mvn compile                          # Compile only
```

## Testing

```bash
mvn test                             # Unit tests only
mvn test -P integration-tests        # Integration tests (requires API keys)
mvn verify -P coverage               # Tests + JaCoCo coverage check (80% min)
```

## Code Formatting

```bash
mvn spotless:check                   # Check formatting
mvn spotless:apply                   # Auto-fix formatting (Palantir Java Format)
```

## System Utils (macOS/Darwin)

```bash
git status / git diff / git log      # Git operations
ls / find / grep                     # File system navigation
```

