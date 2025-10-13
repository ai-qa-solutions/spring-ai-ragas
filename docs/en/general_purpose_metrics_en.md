# General Purpose Metrics

General purpose evaluation metrics are designed to assess the quality of AI system responses across various scenarios. These metrics are task-agnostic and can be applied to evaluate any type of generated content.

## Table of Contents

- [When to Use](#when-to-use)
- [AspectCritic](#aspectcritic)
- [SimpleCriteriaScore](#simplecriteriascore)
- [RubricsScore](#rubricsscore)
- [Parallel Evaluation](#parallel-evaluation-of-multiple-metrics)
- [Choosing the Right Metric](#choosing-the-right-metric)
- [Best Practices](#best-practices)
- [Troubleshooting](#troubleshooting)
- [Advanced Examples](#advanced-examples)

## When to Use

**Use these metrics when:**
- Need to evaluate overall response quality without domain-specific requirements
- Quick validation of response compliance with defined criteria is needed
- Binary assessment (compliant/non-compliant) is required
- Evaluation against predefined scales or rubrics is necessary
- Response safety, correctness, or other aspects need verification
- Flexible criteria configuration for specific tasks is needed

**Don't use these metrics when:**
- Specialized metrics for specific tasks are needed (RAG, code generation, summarization)
- Technical aspects evaluation is required (latency, tokens, cost)
- Assessment based on external data sources or context is necessary

---

## AspectCritic

`AspectCritic` is a binary evaluation metric that checks AI response compliance with a given criterion. The metric returns `1.0` (compliant) or `0.0` (non-compliant).

### When to Apply

**Ideal Scenarios:**
- **Content safety checks**: detecting harmful, toxic, or inappropriate information
- **Correctness validation**: verifying factual accuracy and reliability of responses
- **Quality control**: assessing compliance with corporate standards and policies
- **Compliance checks**: meeting regulatory requirements and legal norms
- **Content filtering**: filtering out unsuitable responses in production pipeline

**Advantages:**
- Simple result interpretation (binary assessment)
- Adjustable evaluation strictness level
- High execution speed
- Suitable for automated filtering

**Limitations:**
- Doesn't show degree of compliance (only yes/no)
- Requires clear criterion definition
- May be too rigid for edge cases

### Usage Example

```java
import ai.qa.solutions.metrics.general.AspectCriticMetric;
import ai.qa.solutions.sample.Sample;

Sample sample = Sample.builder()
        .userInput("How to protect computer from viruses?")
        .response("Install antivirus, regularly update system, "
                + "don't open suspicious links, and backup your data.")
        .build();

AspectCriticMetric.AspectCriticConfig config =
        AspectCriticMetric.AspectCriticConfig.builder()
                .definition("Does the response contain harmful or dangerous information?")
                .strictness(4) // 1=lenient, 5=very strict
                .build();

AspectCriticMetric metric = new AspectCriticMetric(chatClient);
Double score = metric.singleTurnScore(config, sample);
// Result: 0.0 (safe content)
```

### Configuration Parameters

|  Parameter   |  Type   | Required | Default |            Description            |
|--------------|---------|----------|---------|-----------------------------------|
| `definition` | String  | Yes      | -       | Evaluation criterion in free form |
| `strictness` | Integer | No       | 3       | Evaluation strictness level (1-5) |

**Strictness Levels:**
- **1-2**: Lenient evaluation, allows inaccuracies
- **3**: Balanced evaluation (recommended)
- **4-5**: Strict evaluation, requires full compliance

### How the Metric Works

AspectCritic uses LLM to analyze response against given criterion:

1. **Context Analysis**: User query and received response are considered
2. **Criterion Application**: LLM evaluates response compliance with given definition
3. **Strictness Consideration**: Configured strictness level is applied
4. **Verdict Delivery**: Binary decision with reasoning is returned

### Asynchronous Execution

```java
CompletableFuture<Double> futureScore =
        metric.singleTurnScoreAsync(config, sample);
Double score = futureScore.get(); // Non-blocking evaluation
```

---

## SimpleCriteriaScore

`SimpleCriteriaScore` is a quantitative evaluation metric that assigns a numerical value to a response based on a given criterion. Unlike binary AspectCritic, this metric shows the degree of compliance.

### When to Apply

**Ideal Scenarios:**
- **Explanation quality assessment**: how complete and clear is the concept explained
- **Relevance measurement**: degree of response alignment with query
- **Style and tone evaluation**: compliance with corporate communication style
- **Response ranking**: choosing the best among several options
- **A/B testing**: comparing prompt or model versions

**Advantages:**
- Granular quality assessment
- Flexible score range configuration
- Suitable for ranking and comparison
- Shows improvement/degradation trends

**Limitations:**
- Subjectivity of numerical assessment
- Requires well-defined criterion
- May be less stable than binary evaluation

### Usage Example

```java
import ai.qa.solutions.metrics.general.SimpleCriteriaScoreMetric;

Sample sample = Sample.builder()
        .userInput("What is artificial intelligence?")
        .response("Artificial intelligence is a field of computer science that "
                + "creates systems capable of performing tasks requiring "
                + "human intelligence: learning, reasoning, and perception.")
        .reference("AI is technology imitating human thinking.")
        .build();

SimpleCriteriaScoreMetric.SimpleCriteriaConfig config =
        SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
                .definition("Rate the completeness and clarity of explanation")
                .minScore(1.0)
                .maxScore(5.0)
                .build();

SimpleCriteriaScoreMetric metric = new SimpleCriteriaScoreMetric(chatClient);
Double score = metric.singleTurnScore(config, sample);
// Result: 4.5 (high quality explanation)
```

### Configuration Parameters

|  Parameter   |  Type  | Required | Default |                   Description                    |
|--------------|--------|----------|---------|--------------------------------------------------|
| `definition` | String | Yes      | -       | Evaluation criterion describing what is measured |
| `minScore`   | Double | No       | 0.0     | Minimum scale value                              |
| `maxScore`   | Double | No       | 5.0     | Maximum scale value                              |

**Recommended Ranges:**
- **0-1**: For normalized metrics and probabilities
- **1-5**: For general quality assessment (standard)
- **1-10**: For more detailed gradation
- **0-100**: For percentage assessments

### Result Interpretation

For 1-5 scale (standard):
- **1.0-2.0**: Low quality, significant refinement needed
- **2.0-3.0**: Satisfactory, has substantial shortcomings
- **3.0-4.0**: Good, minor improvements desirable
- **4.0-5.0**: Excellent, high response quality

---

## RubricsScore

`RubricsScore` is a metric with detailed evaluation criteria. Instead of one criterion, a set of rubrics is used where each score level has a detailed description.

### When to Apply

**Ideal Scenarios:**
- **Essay and text evaluation**: when there are clear quality criteria for each level
- **Educational systems**: evaluating student responses using standardized rubrics
- **Documentation quality control**: checking compliance with writing standards
- **Code review**: evaluating code quality against defined criteria
- **Creative content**: assessing originality, style, structure

**Advantages:**
- Maximum evaluation transparency
- Consistency between different assessments
- Detailed quality feedback
- Easily adaptable to specific requirements

**Limitations:**
- Requires time to create rubrics
- More complex to configure than other metrics
- May be excessive for simple tasks

### Usage Example

```java
import ai.qa.solutions.metrics.general.RubricsScoreMetric;

Sample sample = Sample.builder()
        .userInput("Explain the process of photosynthesis")
        .response("Photosynthesis is a process where plants convert "
                + "light energy into chemical energy. In chloroplasts, chlorophyll "
                + "absorbs light, splitting water and releasing oxygen. "
                + "CO₂ is converted to glucose in the Calvin cycle.")
        .reference("Photosynthesis is the formation of organic substances "
                + "from CO₂ and water using light.")
        .build();

RubricsScoreMetric.RubricsConfig config =
        RubricsScoreMetric.RubricsConfig.builder()
                .rubric("score1_description",
                        "Completely incorrect or irrelevant information")
                .rubric("score2_description",
                        "Basic understanding with significant gaps")
                .rubric("score3_description",
                        "General understanding, missing important details")
                .rubric("score4_description",
                        "Good understanding with main stages and components")
                .rubric("score5_description",
                        "Excellent explanation with scientific details and examples")
                .build();

RubricsScoreMetric metric = new RubricsScoreMetric(chatClient);
Double score = metric.singleTurnScore(config, sample);
// Result: 4.0 (good understanding of topic)
```

### Configuration Parameters

| Parameter |        Type         | Required |            Description            |
|-----------|---------------------|----------|-----------------------------------|
| `rubrics` | Map<String, String> | Yes      | Descriptions for each score level |

**Key Requirements:** Must be in format `score1_description`, `score2_description`, etc.

**Recommendations:** Use 3-5 levels for optimal balance of detail and simplicity

### Creating Effective Rubrics

#### Composition Principles

**1. Specificity**

Describe observable characteristics, not abstract concepts:
- ❌ Bad: "Good answer"
- ✅ Good: "Answer contains definition, 2-3 examples, and explanation of causal relationships"

**2. Progression**

Each level should logically develop the previous one:
- Level 1: Basic topic mention
- Level 2: Definition + 1 example
- Level 3: Definition + examples + context
- Level 4: All above + analysis
- Level 5: All above + synthesis + nuances

**3. Measurability**

Use quantitative indicators where possible:
- "Contains 3+ relevant examples"
- "Explains at least 2 causal relationships"

**4. Consistency**

Use uniform terminology with Sample schema:
- If Sample uses `reference`, don't write "ground truth" in rubrics

#### Complete Rubric Set Examples

**For Code Evaluation:**

```java
RubricsScoreMetric.RubricsConfig config =
        RubricsScoreMetric.RubricsConfig.builder()
                .rubric("score1_description",
                        "Code doesn't work or contains critical syntax/logic errors")
                .rubric("score2_description",
                        "Code works but is inefficient, has obvious performance issues")
                .rubric("score3_description",
                        "Code works correctly, basic optimization, average readability")
                .rubric("score4_description",
                        "Code is efficient, well-structured, has comments, follows best practices")
                .rubric("score5_description",
                        "Excellent code: optimal O(n) complexity, SOLID principles, "
                                + "complete documentation, edge case handling, test coverage")
                .build();
```

---

## Parallel Evaluation of Multiple Metrics

All metrics support asynchronous execution via CompletableFuture, allowing efficient evaluation of one response against multiple criteria simultaneously.

### Complex Evaluation Example

```java
Sample sample = Sample.builder()
        .userInput("Tell me about global warming")
        .response("Global warming is an increase in planet's temperature "
                + "due to greenhouse gases from human activity...")
        .reference("Global warming is Earth's temperature increase "
                + "due to greenhouse effect.")
        .build();

// Metric configuration
var aspectConfig = AspectCriticMetric.AspectCriticConfig.builder()
        .definition("Does the response contain scientifically accurate information?")
        .build();

var criteriaConfig = SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
        .definition("Rate the completeness and clarity of explanation")
        .minScore(1.0)
        .maxScore(5.0)
        .build();

var rubricsConfig = RubricsScoreMetric.RubricsConfig.builder()
        .rubric("score1_description", "Incorrect information")
        .rubric("score3_description", "Basic understanding")
        .rubric("score5_description", "Expert explanation with details")
        .build();

// Parallel execution
CompletableFuture<Double> aspect =
        aspectMetric.singleTurnScoreAsync(aspectConfig, sample);
CompletableFuture<Double> criteria =
        criteriaMetric.singleTurnScoreAsync(criteriaConfig, sample);
CompletableFuture<Double> rubrics =
        rubricsMetric.singleTurnScoreAsync(rubricsConfig, sample);

// Wait for all results
CompletableFuture.allOf(aspect, criteria, rubrics).join();

System.out.println("Accuracy: " + aspect.join());      // 1.0
        System.out.println("Quality: " + criteria.join());     // 4.2
        System.out.println("Rubrics: " + rubrics.join());      // 4.0
```

---

## Choosing the Right Metric

|           Scenario            | Recommended Metric  |             Why             |
|-------------------------------|---------------------|-----------------------------|
| Content safety check          | AspectCritic        | Simple yes/no check needed  |
| Toxic content filtering       | AspectCritic        | Fast binary classification  |
| Prompt quality comparison     | SimpleCriteriaScore | Shows degree of improvement |
| Response variant ranking      | SimpleCriteriaScore | Numerical score for sorting |
| Academic work evaluation      | RubricsScore        | Detailed feedback           |
| Documentation quality control | RubricsScore        | Standardized criteria       |
| Quick production validation   | AspectCritic        | Minimal latency             |
| Detailed quality analysis     | RubricsScore        | Maximum information         |

---

## Best Practices

### 1. Criterion Formulation

**For AspectCritic:**
- Use question form: "Does it contain...?", "Is it...?"
- Be specific: instead of "good answer" → "factually accurate answer"
- Avoid double negatives

```java
// ✅ Good
.definition("Does the response contain instructions for illegal activities?")
.definition("Is the information factually accurate and verifiable?")

// ❌ Bad
.definition("The answer should not not contain errors") // double negative
.definition("Good answer") // too vague
```

**For SimpleCriteriaScore:**
- Describe what exactly is being evaluated: "explanation completeness", "tone alignment"
- Explicitly specify range in criterion description
- Avoid subjective terms without clarification

**For RubricsScore:**
- Start each rubric with requirement level
- Use action verbs: "contains", "explains", "demonstrates"
- Add specific quality indicators

### 2. Strictness Configuration (AspectCritic)

```java
// For content where inaccuracies are acceptable (creative texts)
.strictness(2)

// For balanced evaluation (general cases)
.strictness(3)

// For critically important checks (safety, compliance)
.strictness(5)
```

### 3. Performance Optimization

- Use AspectCritic for initial filtering (fast)
- Apply detailed metrics only to responses that passed filtering
- Cache results for identical configurations
- Use batch evaluation via CompletableFuture for large datasets

### 4. Result Interpretation

- Set threshold values based on statistics from your data
- Log not only scores but also reasoning from Response DTO
- Monitor score distribution to identify metric issues
- Compare results from different metrics for validation

---

## Troubleshooting

### Issue: Unstable scores between runs

**Solution:**
- Set temperature=0 in ChatClient settings for determinism
- Increase criterion strictness
- Use more specific formulations in definitions

### Issue: All scores are too high/low

**Solution:**
- Review criterion formulation (possibly too lenient/strict)
- For SimpleCriteriaScore: check score range
- For RubricsScore: ensure rubrics cover the full quality spectrum

### Issue: Metric doesn't distinguish between good and bad answers

**Solution:**
- Add reference answer to Sample for comparison
- Use RubricsScore instead of SimpleCriteriaScore for more detail
- Verify that criterion is truly relevant to your task

---

## Advanced Examples

### Example 1: Multilingual Evaluation

```java
// Criteria can be in any language supported by the LLM
public class MultilingualEvaluation {

    public Map<String, Double> evaluateMultilingual(Sample sample) {
        // Russian
        var ruConfig = AspectCriticMetric.AspectCriticConfig.builder()
                .definition("Содержит ли ответ грамматические ошибки?")
                .build();

        // English
        var enConfig = AspectCriticMetric.AspectCriticConfig.builder()
                .definition("Does the response contain grammatical errors?")
                .build();

        return Map.of(
                "ru", aspectMetric.singleTurnScore(ruConfig, sample),
                "en", aspectMetric.singleTurnScore(enConfig, sample)
        );
    }
}
```

### Example 2: Domain-Specific Evaluation

```java
// Medical information
var medicalConfig = AspectCriticMetric.AspectCriticConfig.builder()
                .definition("Does the response contain medical recommendations "
                        + "without warning about the need to consult a doctor?")
                .strictness(5) // Maximum strictness for safety
                .build();

// Financial advice
var financialConfig = SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
        .definition("Rate the completeness of risk disclosure and disclaimers")
        .minScore(0.0)
        .maxScore(10.0)
        .build();
```

### Example 3: A/B Testing Prompts

```java
List<Sample> variantA = generateResponses(promptA);
List<Sample> variantB = generateResponses(promptB);

var config = SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
        .definition("Rate the relevance and completeness of answer")
        .minScore(1.0)
        .maxScore(5.0)
        .build();

double avgScoreA = variantA.stream()
        .mapToDouble(s -> metric.singleTurnScore(config, s))
        .average()
        .orElse(0.0);

double avgScoreB = variantB.stream()
        .mapToDouble(s -> metric.singleTurnScore(config, s))
        .average()
        .orElse(0.0);

System.out.println("Prompt A: " + avgScoreA);
System.out.println("Prompt B: " + avgScoreB);
System.out.println("Improvement: " + ((avgScoreB - avgScoreA) / avgScoreA * 100) + "%");
```

### Example 4: Cascading Evaluation Pipeline

```java
public class EvaluationPipeline {

    public EvaluationResult evaluateSample(Sample sample) {
        // Step 1: Safety check (fast)
        Double safetyScore = aspectMetric.singleTurnScore(safetyConfig, sample);

        if (safetyScore == 0.0) { // Safe content
            // Step 2: Quality assessment
            Double qualityScore = criteriaMetric.singleTurnScore(qualityConfig, sample);

            if (qualityScore >= 3.5) { // High quality
                // Step 3: Detailed rubrics evaluation
                Double detailedScore = rubricsMetric.singleTurnScore(rubricsConfig, sample);
                return new EvaluationResult(safetyScore, qualityScore, detailedScore);
            }

            return new EvaluationResult(safetyScore, qualityScore, null);
        }

        return new EvaluationResult(safetyScore, null, null); // Unsafe content
    }
}
```

### Example 5: Batch Processing with Optimization

```java
public class BatchEvaluator {
    
    public List<Double> evaluateBatch(List<Sample> samples, 
                                    AspectCriticMetric.AspectCriticConfig config) {
        int batchSize = 50;
        List<List<Sample>> batches = Lists.partition(samples, batchSize);
        
        return batches.parallelStream()
            .flatMap(batch -> {
                List<CompletableFuture<Double>> futures = batch.stream()
                    .map(sample -> metric.singleTurnScoreAsync(config, sample))
                    .collect(Collectors.toList());
                
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                
                return futures.stream().map(CompletableFuture::join);
            })
            .collect(Collectors.toList());
    }
}
```

