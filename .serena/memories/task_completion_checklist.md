# Task Completion Checklist

After completing any task, verify:

1. **Format code:** `mvn spotless:apply`
2. **Run unit tests:** `mvn test`
3. **Check coverage** (if modified metrics): `mvn verify -P coverage` (80% min)
4. **Validate Allure reports** (if modified metrics/allure): run integration test and check HTML attachment

## When Adding a New Metric

1. Create config class with `@Builder` in metrics module
2. Extend `AbstractMultiModelMetric<YourConfig>`
3. Implement `singleTurnScoreAsync()` with proper notifier calls
4. Add Spring bean in `RagasMetricsAutoconfiguration`
5. Write unit tests using `StubMultiModelExecutor`
6. Add integration tests for `en/` and `ru/`
7. Add Allure explanation class and methodology docs
8. Run `mvn spotless:apply` and `mvn test`

