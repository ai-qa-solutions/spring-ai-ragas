# Spring AI RAGAS Allure Integration

<!-- TOC -->
* [Spring AI RAGAS Allure Integration](#spring-ai-ragas-allure-integration)
  * [Features](#features)
  * [Report Preview](#report-preview)
  * [Installation](#installation)
  * [Quick Start](#quick-start)
    * [1. Enable Allure Integration](#1-enable-allure-integration)
    * [2. Run Tests and View Reports](#2-run-tests-and-view-reports)
  * [Configuration Reference](#configuration-reference)
  * [Report Contents](#report-contents)
    * [Summary Block](#summary-block)
    * [Score Explanation Block](#score-explanation-block)
    * [Execution Timeline](#execution-timeline)
    * [Methodology Documentation](#methodology-documentation)
  * [Score Explanations by Metric](#score-explanations-by-metric)
  * [Advanced Usage](#advanced-usage)
    * [Manual Listener Registration](#manual-listener-registration)
    * [Custom Template Overrides](#custom-template-overrides)
    * [Accessing Report Data Programmatically](#accessing-report-data-programmatically)
  * [API Reference](#api-reference)
    * [AllureMetricExecutionListener](#alluremetricexecutionlistener)
    * [EvaluationReportData](#evaluationreportdata)
    * [ChartData](#chartdata)
  * [Multilingual Support](#multilingual-support)
  * [Examples](#examples)
    * [Integration Test with Allure](#integration-test-with-allure)
    * [Custom Listener with Additional Processing](#custom-listener-with-additional-processing)
    * [Programmatic Report Generation](#programmatic-report-generation)

<!-- TOC -->

-------------

Allure Report integration for Spring AI RAGAS metrics evaluation. This module automatically generates rich, interactive
HTML and Markdown attachments for each metric evaluation, providing detailed insights into AI response quality
assessment.

## Features

- **Automatic Report Generation** - HTML and Markdown attachments for each metric evaluation
- **SVG Visualizations** - Interactive charts for score comparison and execution timeline
- **Score Explanations** - Detailed breakdown of how each metric calculated its score
- **Methodology Documentation** - Built-in documentation explaining each metric's methodology
- **Multilingual Support** - Full support for English and Russian reports
- **Spring Boot Autoconfiguration** - Zero-code setup with `spring.ai.ragas.allure.enabled=true`
- **Thread-Safe** - Safe for parallel metric execution with per-evaluation listener instances

## Report Preview

Here's an example of a generated Allure report for ResponseRelevancyMetric:

![ResponseRelevancyMetric Report Example](example/ResponseRelevancyMetric_example.png)

[View interactive HTML example](example/data_attachments_811c9d3a3f38a65a.html)

## Installation

Add the dependency to your `pom.xml`

```xml

<dependency>
    <groupId>io.github.ai-qa-solutions</groupId>
    <artifactId>spring-ai-ragas-allure</artifactId>
    <version>LATEST</version>
</dependency>
```

Also ensure you have Allure dependencies for your test framework

```xml

<dependency>
    <groupId>io.qameta.allure</groupId>
    <artifactId>allure-junit5</artifactId>
    <version>2.25.0</version>
    <scope>test</scope>
</dependency>
```

Or for Gradle

```gradle
implementation 'io.github.ai-qa-solutions:spring-ai-ragas-allure:1.0.0'
testImplementation 'io.qameta.allure:allure-junit5:2.25.0'
```

## Quick Start

### 1. Enable Allure Integration

Add to your `application.yml`

```yaml
spring:
  ai:
    ragas:
      allure:
        enabled: true
        language: en  # or "ru" for Russian
        include-prompts: true
        include-responses: true
```

### 2. Run Tests and View Reports

```bash
# Run tests with Allure
mvn clean test

# Generate and serve Allure report
mvn allure:serve
```

The report will automatically include detailed attachments for each metric evaluation.

## Configuration Reference

|                   Property                    |  Type   | Default |              Description               |
|-----------------------------------------------|---------|---------|----------------------------------------|
| `spring.ai.ragas.allure.enabled`              | Boolean | false   | Enable Allure integration              |
| `spring.ai.ragas.allure.language`             | String  | "en"    | Report language ("en" or "ru")         |
| `spring.ai.ragas.allure.include-prompts`      | Boolean | true    | Include full LLM prompts in reports    |
| `spring.ai.ragas.allure.include-responses`    | Boolean | true    | Include full JSON responses in reports |
| `spring.ai.ragas.allure.include-stack-traces` | Boolean | true    | Include stack traces for failed models |

## Report Contents

Each metric evaluation generates an attachment containing:

### Summary Block

- **Metric Name** - The evaluated metric
- **Aggregated Score** - Final score with visual indicator (color-coded)
- **Model Scores** - Individual scores from each model
- **Execution Duration** - Total evaluation time
- **Sample Data** - User input, AI response, contexts, reference

### Score Explanation Block

Metric-specific breakdown showing:

- **Step-by-step calculation** - How the score was derived
- **Intermediate results** - Statements, verdicts, entities, etc.
- **Model-specific data** - Results from each model
- **Visual indicators** - Pass/fail icons, progress bars

### Execution Timeline

SVG visualization showing:

- **Parallel execution** - How models ran concurrently
- **Step timing** - Duration of each evaluation step
- **Model performance** - Comparative execution times
- **Error indicators** - Failed models highlighted

### Methodology Documentation

Built-in documentation for each metric explaining:

- What the metric measures
- How the score is calculated
- Score interpretation guidelines
- Example use cases

## Score Explanations by Metric

|          Metric           |                Explanation Contents                 |
|---------------------------|-----------------------------------------------------|
| AspectCriticMetric        | Criteria, verdict, reasoning, strictness iterations |
| SimpleCriteriaScoreMetric | Raw scores, normalized score, reasoning             |
| RubricsScoreMetric        | Rubric levels, selected level, reasoning            |
| FaithfulnessMetric        | Statements extracted, verdicts per statement        |
| ContextPrecisionMetric    | Context relevance per chunk, precision@k values     |
| ContextRecallMetric       | Reference classifications, found/missing statements |
| ContextEntityRecallMetric | Reference entities, context entities, matches       |
| NoiseSensitivityMetric    | Reference vs response statements, matching analysis |
| ResponseRelevancyMetric   | Generated questions, similarity scores              |

## Advanced Usage

### Manual Listener Registration

If you need more control over listener registration

```java

@Service
public class MetricService {

    private final AllureMetricExecutionListener allureListener;

    public MetricService(AllureMetricExecutionListener allureListener,
                         MultiModelExecutor executor) {
        this.allureListener = allureListener;
        this.faithfulnessMetric = FaithfulnessMetric.builder()
                .executor(executor)
                .build()
                .withListeners(List.of(allureListener));
    }

    public Double evaluate(Sample sample) {
        return faithfulnessMetric.singleTurnScore(sample);
    }
}
```

### Custom Template Overrides

Create custom Freemarker templates in your resources

```
src/main/resources/
  ai/qa/solutions/allure/templates/
    custom-report.ftl
```

```java

@Configuration
public class CustomAllureConfig {

    @Bean
    public FreemarkerTemplateEngine customTemplateEngine() {
        FreemarkerTemplateEngine engine = new FreemarkerTemplateEngine();
        // Configure custom template location
        return engine;
    }
}
```

### Accessing Report Data Programmatically

```java

@Service
public class ReportExportService {

    private final FreemarkerTemplateEngine templateEngine;

    public String generateCustomReport(EvaluationReportData reportData) {
        // Access all report data
        String metricName = reportData.getMetricName();
        Double score = reportData.getAggregatedScore();
        Map<String, Double> modelScores = reportData.getModelScores();
        List<StepExecutionData> steps = reportData.getSteps();

        // Generate custom format
        return templateEngine.renderHtml(reportData);
    }
}
```

## API Reference

### AllureMetricExecutionListener

Main listener that integrates with Allure lifecycle.

|           Method           |                   Description                    |
|----------------------------|--------------------------------------------------|
| `beforeMetricEvaluation()` | Captures evaluation context, creates Allure step |
| `afterStep()`              | Records step execution data and timing           |
| `onModelExcluded()`        | Records model exclusion events                   |
| `afterMetricEvaluation()`  | Generates and attaches HTML/MD reports           |
| `forEvaluation()`          | Returns new instance for thread-safe execution   |
| `getOrder()`               | Returns 100 (runs after logging listener)        |

### EvaluationReportData

Complete data container for report generation.

|         Field         |           Type           |         Description         |
|-----------------------|--------------------------|-----------------------------|
| `metricName`          | String                   | Name of the metric          |
| `userInput`           | String                   | User's question/input       |
| `response`            | String                   | AI response being evaluated |
| `reference`           | String                   | Ground truth (optional)     |
| `retrievedContexts`   | List<String>             | Retrieved context chunks    |
| `aggregatedScore`     | Double                   | Final aggregated score      |
| `modelScores`         | Map<String, Double>      | Scores by model ID          |
| `steps`               | List<StepExecutionData>  | Step-by-step execution data |
| `exclusions`          | List<ModelExclusionData> | Model exclusion events      |
| `chartData`           | ChartData                | Visualization data          |
| `scoreExplanation`    | ScoreExplanation         | Metric-specific explanation |
| `methodologyHtml`     | String                   | Methodology as HTML         |
| `methodologyMarkdown` | String                   | Methodology as Markdown     |
| `totalDuration`       | Duration                 | Total evaluation duration   |

### ChartData

Data for SVG chart generation.

|       Field        |        Type         |         Description          |
|--------------------|---------------------|------------------------------|
| `scoreEntries`     | List<ScoreEntry>    | Model scores for bar chart   |
| `timelineEntries`  | List<TimelineEntry> | Execution timeline data      |
| `maxDurationMs`    | Long                | Maximum duration for scaling |
| `timeScale`        | Double              | Scale factor for timeline    |
| `heatmapRowLabels` | List<String>        | Row labels for heatmap       |
| `heatmapColLabels` | List<String>        | Column labels for heatmap    |
| `heatmapValues`    | List<List<Integer>> | Heatmap cell values (0/1/-1) |

## Multilingual Support

The module includes full translations for English and Russian

**English (`language: en`)**

- Report titles and labels
- Score interpretations
- Methodology documentation

**Russian (`language: ru`)**

- Russian report titles and labels
- Russian score interpretations
- Russian methodology documentation

Methodology files are located in:

```
src/main/resources/ai/qa/solutions/allure/methodology/
  en/
    faithfulness.md
    context-precision.md
    ...
  ru/
    faithfulness.md
    context-precision.md
    ...
```

## Examples

### Integration Test with Allure

```java

@SpringBootTest
@ExtendWith(AllureJunit5.class)
class RAGEvaluationIT {

    @Autowired
    private FaithfulnessMetric faithfulnessMetric;

    @Autowired
    private ContextPrecisionMetric contextPrecisionMetric;

    @Test
    @DisplayName("Evaluate RAG response quality")
    void shouldEvaluateRAGResponse() {
        // Given
        Sample sample = Sample.builder()
                .userInput("What is the capital of France?")
                .response("Paris is the capital of France.")
                .retrievedContexts(List.of(
                        "Paris is the capital and largest city of France.",
                        "France is a country in Western Europe."
                ))
                .build();

        // When - Allure attachments are generated automatically
        Double faithfulness = faithfulnessMetric.singleTurnScore(sample);
        Double precision = contextPrecisionMetric.singleTurnScore(sample);

        // Then
        assertThat(faithfulness).isGreaterThan(0.8);
        assertThat(precision).isGreaterThan(0.5);

        // Allure report will contain:
        // - FaithfulnessMetric attachment with statement verdicts
        // - ContextPrecisionMetric attachment with context relevance
    }
}
```

### Custom Listener with Additional Processing

```java

@Component
public class MetricsCollectorListener implements MetricExecutionListener {

    private final MeterRegistry meterRegistry;
    private final AllureMetricExecutionListener allureListener;

    @Override
    public void afterMetricEvaluation(MetricEvaluationResult result) {
        // Record metrics to Prometheus/Micrometer
        meterRegistry.gauge(
                "ragas.metric.score",
                Tags.of("metric", result.getMetricName()),
                result.getAggregatedScore()
        );

        // Allure listener handles report generation automatically
    }

    @Override
    public MetricExecutionListener forEvaluation() {
        return new MetricsCollectorListener(meterRegistry, allureListener);
    }

    @Override
    public int getOrder() {
        return 200; // Run after Allure listener
    }
}
```

### Programmatic Report Generation

```java

@Service
public class ReportService {

    private final FreemarkerTemplateEngine templateEngine;

    public void exportReports(List<EvaluationReportData> reports) {
        for (EvaluationReportData report : reports) {
            // Generate HTML
            String html = templateEngine.renderHtml(report);
            saveToFile(report.getMetricName() + ".html", html);

            // Generate Markdown
            String markdown = templateEngine.renderMarkdown(report);
            saveToFile(report.getMetricName() + ".md", markdown);

            // Access score explanation
            if (report.hasScoreExplanation()) {
                ScoreExplanation explanation = report.getScoreExplanation();
                processExplanation(explanation);
            }
        }
    }
}
```

