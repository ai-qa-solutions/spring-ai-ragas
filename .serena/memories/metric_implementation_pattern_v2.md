# Metric Implementation Pattern v2

## LLM-based Metrics

All LLM metrics extend `AbstractMultiModelMetric<Config>`:

```java
public class MyMetric extends AbstractMultiModelMetric<MyConfig> {

    @Builder
    public MyMetric(MultiModelExecutor executor) {
        super(executor);
    }

    @Override
    public String getName() { return "MyMetric"; }

    @Override
    public CompletableFuture<Double> singleTurnScoreAsync(MyConfig config, Sample sample) {
        final EvaluationNotifier notifier = createEvaluationNotifier();
        final List<String> modelIds = executor.getModelIds();
        
        notifier.beforeMetricEvaluation(MetricEvaluationContext.builder()
            .metricName(getName())
            .sample(sample)
            .config(config)
            .modelIds(modelIds)
            .totalSteps(2)
            .build());
        // NOTE: MetricEvaluationContext has NO metadata field (removed in Feb 2026 refactoring)

        return executor.runAsync(() -> {
            final List<StepResults> steps = new ArrayList<>();
            final List<ModelExclusionEvent> exclusions = new ArrayList<>();
            Double score = null;
            
            try {
                // Step 1: LLM call
                StepResults step1 = executor.executeLlm(modelIds, prompt, ResponseType.class);
                steps.add(step1);
                
                // Step 2: Another LLM call or computation
                StepResults step2 = executor.executeLlm(modelIds, prompt2, ResponseType2.class);
                steps.add(step2);
                
                // Aggregate
                Map<String, Double> modelScores = computeScoresPerModel(step1, step2);
                score = aggregate(modelScores);
                
                return score;
            } finally {
                notifier.afterMetricEvaluation(MetricEvaluationResult.builder()
                    .metricName(getName())
                    .sample(sample)
                    .config(config)
                    .modelIds(modelIds)
                    .aggregatedScore(score)
                    .steps(steps)
                    .exclusions(exclusions)
                    .metadata(new MyMetricMetadata /* must implement MetricMetadata */(/* typed fields */))
                    .build());
            }
        });
    }
}
```

## NLP Metrics (Non-LLM)

NLP metrics extend `AbstractMetric<Config>` directly:

```java
public class MyNlpMetric extends AbstractMetric<MyNlpConfig> {

    @Override
    public String getName() { return "MyNlpMetric"; }

    @Override
    public Double singleTurnScore(MyNlpConfig config, Sample sample) {
        final EvaluationNotifier notifier = createEvaluationNotifier();
        notifier.beforeMetricEvaluation(MetricEvaluationContext.builder()
            .metricName(getName())
            .sample(sample)
            .config(config)
            .modelIds(List.of())
            .totalSteps(0)
            .build());

        Double score = null;
        try {
            // Validate input
            if (sample.getResponse() == null || sample.getResponse().isEmpty()) {
                return null;  // early return — finally block still runs
            }
            
            score = computeScore(sample, config);
            return score;
        } finally {
            notifier.afterMetricEvaluation(MetricEvaluationResult.builder()
                .metricName(getName())
                .sample(sample)
                .config(config)
                .modelIds(List.of())
                .aggregatedScore(score)
                .metadata(new MyNlpMetadata /* must implement MetricMetadata, NO response/reference fields */(/* typed fields */))
                .build());
        }
    }

    @Override
    public CompletableFuture<Double> singleTurnScoreAsync(MyNlpConfig config, Sample sample) {
        return CompletableFuture.completedFuture(singleTurnScore(config, sample));
    }
}
```

## Agent Metrics (Multi-turn)

Agent metrics extend `AbstractMultiTurnMetric<Config>`:
- Override `multiTurnScoreAsync()` instead of `singleTurnScoreAsync()`
- Use typed message hierarchy: `HumanMessage`, `AIMessage`, `ToolMessage`

## Rich Evaluation API

All metrics also support `singleTurnEvaluate()` / `multiTurnEvaluate()` returning `EvaluationResult` (instead of `Double`):

```java
// Rich result with explanation, metadata, per-model scores
EvaluationResult result = metric.singleTurnEvaluate(config, sample);
result.getScore();           // aggregated Double
result.getExplanation();     // ScoreExplanation (from ai.qa.solutions.metric.explanation)
result.getModelScores();     // Map<String, Double>
result.getMetadata();        // typed MetricMetadata
result.getTotalDuration();   // Duration

// Async
CompletableFuture<EvaluationResult> future = metric.singleTurnEvaluateAsync(config, sample);

// Multi-turn agent metrics
EvaluationResult agentResult = metric.multiTurnEvaluate(config, sample);
```

Implemented in `AbstractMetric` using a `ResultCapturingListener` that intercepts `MetricEvaluationResult` during `singleTurnScore()`. Explanation is built eagerly via `ScoreExplanationFactory.create(result, config.getLanguage())`.

**Language support:** All config classes have `@Builder.Default private String language = "en"`. Set `.language("ru")` for Russian explanations.

## Key Rules

1. **Always use try/finally** for lifecycle guarantees (especially NLP early returns)
2. **Typed metadata records** — never use `Map<String, Object>`, create a specific record implementing `MetricMetadata` interface from `ai.qa.solutions.execution.listener.dto`
3. **Steps accumulated internally** — each metric builds its own `List<StepResults>`
4. **Listeners via withListeners()** — autoconfiguration injects listeners
5. **EvaluationNotifier per evaluation** — call `createEvaluationNotifier()` for thread safety
6. **totalDuration may be null** — especially for NLP metrics (no LLM calls)

## Spring Boot Autoconfiguration

```java
@Bean
public MyMetric myMetric(MultiModelExecutor executor, List<MetricExecutionListener> listeners) {
    return MyMetric.builder().executor(executor).build().withListeners(listeners);
}

// NLP metrics don't need executor
@Bean
public MyNlpMetric myNlpMetric(List<MetricExecutionListener> listeners) {
    return new MyNlpMetric().withListeners(listeners);
}
```

