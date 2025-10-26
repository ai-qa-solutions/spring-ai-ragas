# Retrieval Metrics

Retrieval metrics are specialized evaluation tools designed to assess the performance of retrieval-augmented
generation (RAG) systems. These metrics evaluate how well retrieved contexts support answer generation and measure
various aspects of retrieval quality.

## Table of Contents

- [When to Use](#when-to-use)
- [ContextEntityRecall](#contextentityrecall)
- [ContextPrecision](#contextprecision)
- [ContextRecall](#contextrecall)
- [Faithfulness](#faithfulness)
- [NoiseSensitivity](#noisesensitivity)
- [ResponseRelevancy](#responserelevancy)
- [Choosing the Right Metric](#choosing-the-right-metric)
- [Best Practices](#best-practices)
- [Troubleshooting](#troubleshooting)
- [Advanced Examples](#advanced-examples)

## When to Use

**Use retrieval metrics when:**

- Evaluating RAG system performance
- Assessing retrieval mechanism quality
- Measuring factual consistency between responses and contexts
- Optimizing retrieval parameters and strategies
- Comparing different retrieval approaches
- Detecting hallucinations and factual errors
- Evaluating entity coverage in fact-based applications

**Don't use these metrics when:**

- Evaluating general response quality without retrieval context
- Assessing creative or subjective content where factual accuracy isn't primary
- Working with systems that don't use retrieved contexts
- Measuring technical performance aspects (latency, cost)

---

## ContextEntityRecall

`ContextEntityRecall` measures the recall of entities present in both reference and retrieved contexts relative to
entities in reference alone. This metric is particularly useful for fact-based applications like tourism help desks or
historical QA systems.

### When to Apply

**Ideal Scenarios:**

- **Tourism and travel systems**: Ensuring all mentioned places, dates, and attractions are covered
- **Historical QA systems**: Verifying coverage of people, dates, events, and locations
- **Educational content**: Checking if all key terms and concepts are retrievable
- **News and information retrieval**: Ensuring comprehensive entity coverage
- **Knowledge base evaluation**: Assessing completeness of factual information

**Advantages:**

- Focuses on concrete, verifiable entities
- Language-agnostic evaluation approach
- Useful for measuring retrieval comprehensiveness
- Handles different entity types (people, places, dates, numbers)

**Limitations:**

- Only measures entity coverage, not semantic completeness
- May miss important conceptual information
- Requires clear entity extraction capabilities

### Usage Example

```java
import ai.qa.solutions.metrics.retrieval.ContextEntityRecallMetric;
import ai.qa.solutions.sample.Sample;

Sample sample = Sample.builder()
        .reference("The Eiffel Tower is located in Paris, France. It was completed in 1889 for the World's Fair.")
        .retrievedContexts(List.of(
                "The Eiffel Tower, located in Paris, France, is one of the most iconic landmarks globally.",
                "Completed in 1889, it was constructed in time for the 1889 World's Fair.",
                "Millions of visitors are attracted to it each year for its breathtaking views of the city."))
        .build();

ContextEntityRecallMetric.ContextEntityRecallConfig config =
        ContextEntityRecallMetric.ContextEntityRecallConfig.builder().build();

ContextEntityRecallMetric metric = new ContextEntityRecallMetric(chatClient);
Double score = metric.singleTurnScore(config, sample);
// Result: ~0.9 (high entity coverage)
```

### How the Metric Works

1. **Entity Extraction**: Extracts named entities from reference answer using LLM
2. **Context Analysis**: Extracts entities from retrieved contexts
3. **Recall Calculation**: Computes overlap between reference and context entities
4. **Score Computation**: Returns ratio of covered entities to total reference entities

**Entity Types Detected:**

- Person names (Albert Einstein, Napoleon)
- Place names (Paris, Eiffel Tower, France)
- Organizations (UNESCO, European Union)
- Dates and times (1889, July 16, 1969)
- Events (World War II, Apollo 11 mission)
- Products/objects (iPhone, Great Wall of China)
- Numbers and measurements (21,196 kilometers, 50,000 spectators)

### Result Interpretation

- **0.9-1.0**: Excellent entity coverage, most entities found
- **0.7-0.9**: Good coverage, minor entities may be missing
- **0.5-0.7**: Moderate coverage, several important entities missing
- **0.3-0.5**: Poor coverage, significant gaps in entity information
- **0.0-0.3**: Very poor coverage, most entities not found

---

## ContextPrecision

`ContextPrecision` evaluates the retriever's ability to rank relevant chunks higher in the retrieved context list. It
automatically chooses between reference-based or response-based evaluation depending on available data.

### When to Apply

**Ideal Scenarios:**

- **Retrieval system optimization**: Measuring ranking quality of retrieved documents
- **Search relevance evaluation**: Assessing how well relevant documents are prioritized
- **RAG system tuning**: Optimizing retrieval parameters for better precision
- **Comparative retrieval analysis**: Comparing different retrieval strategies
- **Quality control**: Monitoring retrieval performance in production

**Advantages:**

- Adapts to available data (reference or response-based)
- Considers ranking order, not just presence of relevant content
- Provides nuanced evaluation of retrieval quality
- Suitable for both supervised and unsupervised scenarios

**Limitations:**

- Requires multiple retrieved contexts for meaningful evaluation
- May be sensitive to LLM evaluation consistency
- Doesn't measure recall aspects of retrieval

### Usage Example

```java
import ai.qa.solutions.metrics.retrieval.ContextPrecisionMetric;

Sample sample = Sample.builder()
        .userInput("What is machine learning?")
        .reference("Machine learning is a branch of AI that uses data and algorithms to learn.")
        .retrievedContexts(List.of(
                "Machine learning is a branch of artificial intelligence focused on data analysis.",
                "Weather forecast: sunny with 25°C temperature today.",
                "ML algorithms can learn patterns from data without explicit programming.",
                "Basketball is a popular sport played worldwide."))
        .build();

ContextPrecisionMetric.ContextPrecisionConfig config =
        ContextPrecisionMetric.ContextPrecisionConfig.builder()
                .evaluationStrategy(ContextPrecisionMetric.EvaluationStrategy.REFERENCE_BASED)
                .build();

ContextPrecisionMetric metric = new ContextPrecisionMetric(chatClient);
Double score = metric.singleTurnScore(config, sample);
// Result: ~0.6 (good relevant chunks ranked higher)
```

### Configuration Parameters

|      Parameter       |        Type        | Required |   Default   |            Description            |
|----------------------|--------------------|----------|-------------|-----------------------------------|
| `evaluationStrategy` | EvaluationStrategy | No       | Auto-detect | REFERENCE_BASED or RESPONSE_BASED |

**Evaluation Strategies:**

- **REFERENCE_BASED**: Uses reference answer as gold standard (preferred when available)
- **RESPONSE_BASED**: Uses AI response for relevance evaluation
- **Auto-detect**: Chooses REFERENCE_BASED if reference is available, otherwise RESPONSE_BASED

### How the Metric Works

1. **Relevance Assessment**: Each retrieved context chunk is evaluated for relevance
2. **Precision Calculation**: For each position k, calculates precision@k
3. **Average Precision**: Computes mean precision across all positions
4. **Final Score**: Returns average precision as context precision score

### Result Interpretation

- **0.8-1.0**: Excellent precision, relevant contexts consistently ranked high
- **0.6-0.8**: Good precision, most relevant contexts ranked appropriately
- **0.4-0.6**: Moderate precision, mixed ranking quality
- **0.2-0.4**: Poor precision, relevant contexts often ranked low
- **0.0-0.2**: Very poor precision, minimal ranking effectiveness

---

## ContextRecall

`ContextRecall` measures how many statements in the reference answer can be attributed to the retrieved contexts. This
metric is essential for evaluating the completeness of retrieved information.

### When to Apply

**Ideal Scenarios:**

- **Comprehensive information retrieval**: Ensuring all needed information is accessible
- **Knowledge base completeness**: Verifying that reference answers are fully supported
- **RAG system evaluation**: Measuring information coverage quality
- **Retrieval gap analysis**: Identifying missing information in retrieved contexts
- **Academic and research applications**: Ensuring thorough information coverage

**Advantages:**

- Provides detailed statement-level analysis
- Measures information completeness effectively
- Useful for identifying retrieval gaps
- Works well with factual, reference-based content

**Limitations:**

- Requires reference answers for evaluation
- May be strict in attribution requirements
- Performance depends on statement decomposition quality

### Usage Example

```java
import ai.qa.solutions.metrics.retrieval.ContextRecallMetric;

Sample sample = Sample.builder()
        .userInput("Tell me about photosynthesis")
        .reference("Photosynthesis converts light energy to chemical energy. " +
                "It occurs in chloroplasts. The process requires CO₂, water, and sunlight. " +
                "Oxygen is released as a byproduct.")
        .retrievedContexts(List.of(
                "Photosynthesis is a process where plants convert sunlight into chemical energy.",
                "Chloroplasts are organelles in plant cells where photosynthesis occurs.",
                "During photosynthesis, oxygen is produced as a waste product.",
                "The process requires carbon dioxide, water, and light energy."))
        .build();

ContextRecallMetric.ContextRecallConfig config =
        ContextRecallMetric.ContextRecallConfig.builder().build();

ContextRecallMetric metric = new ContextRecallMetric(chatClient);
Double score = metric.singleTurnScore(config, sample);
// Result: ~0.95 (almost all statements supported)
```

### How the Metric Works

1. **Statement Decomposition**: Breaks reference answer into individual sentences
2. **Attribution Analysis**: Evaluates each statement against retrieved contexts
3. **Support Classification**: Determines if each statement can be attributed to contexts
4. **Recall Calculation**: Returns ratio of attributable statements to total statements

### Result Interpretation

- **0.9-1.0**: Excellent recall, nearly all information retrievable
- **0.7-0.9**: Good recall, most key information available
- **0.5-0.7**: Moderate recall, some important information missing
- **0.3-0.5**: Poor recall, significant information gaps
- **0.0-0.3**: Very poor recall, most information not retrievable

---

## Faithfulness

`Faithfulness` measures factual consistency between the generated response and retrieved contexts. It identifies
hallucinations and ensures responses are grounded in provided information.

### When to Apply

**Ideal Scenarios:**

- **Hallucination detection**: Identifying factually incorrect or unsupported claims
- **RAG system validation**: Ensuring responses stay grounded in retrieved contexts
- **Quality assurance**: Maintaining factual accuracy in AI responses
- **Medical/legal applications**: Critical domains requiring factual precision
- **News and information systems**: Ensuring accurate information dissemination

**Advantages:**

- Detects both factual errors and hallucinations
- Provides detailed statement-level analysis
- Works with various types of factual content
- Essential for maintaining response reliability

**Limitations:**

- Requires retrieved contexts for evaluation
- May be sensitive to statement decomposition quality
- Performance depends on LLM's factual reasoning abilities

### Usage Example

```java
import ai.qa.solutions.metrics.retrieval.FaithfulnessMetric;

Sample sample = Sample.builder()
        .userInput("When was the first Super Bowl?")
        .response("The first Super Bowl was held on January 15, 1967.")
        .retrievedContexts(List.of(
                "The first Super Bowl was held on January 15, 1967, at the Los Angeles Memorial Coliseum."))
        .build();

FaithfulnessMetric metric = new FaithfulnessMetric(chatClient);
Double score = metric.singleTurnScore(sample);
// Result: 1.0 (perfectly faithful response)
```

### How the Metric Works

1. **Statement Generation**: Decomposes response into atomic statements
2. **Faithfulness Evaluation**: Checks each statement against retrieved contexts
3. **Verdict Assignment**: Determines if statements can be inferred from contexts
4. **Score Computation**: Returns ratio of faithful statements to total statements

### Result Interpretation

- **0.9-1.0**: Excellent faithfulness, response well-grounded in contexts
- **0.7-0.9**: Good faithfulness, minor inconsistencies or gaps
- **0.5-0.7**: Moderate faithfulness, some unsupported claims present
- **0.3-0.5**: Poor faithfulness, significant hallucinations detected
- **0.0-0.3**: Very poor faithfulness, response largely unsupported

---

## NoiseSensitivity

`NoiseSensitivity` measures how often a system makes errors by providing incorrect responses when utilizing either
relevant or irrelevant retrieved documents. Lower scores indicate better performance.

### When to Apply

**Ideal Scenarios:**

- **Robustness testing**: Evaluating system behavior with noisy retrieval results
- **Error analysis**: Understanding how irrelevant contexts affect response quality
- **Retrieval quality impact**: Measuring sensitivity to retrieval accuracy
- **System optimization**: Improving response generation despite noisy inputs
- **Production monitoring**: Detecting degradation due to retrieval issues

**Advantages:**

- Measures system robustness to noisy inputs
- Distinguishes between relevant and irrelevant context impacts
- Useful for understanding retrieval-generation interaction
- Helps identify system vulnerabilities

**Limitations:**

- Requires both reference answers and retrieved contexts
- Complex metric with multiple evaluation steps
- May be sensitive to evaluation prompt design

### Usage Example

```java
import ai.qa.solutions.metrics.retrieval.NoiseSensitivityMetric;

Sample sample = Sample.builder()
        .userInput("What causes earthquakes?")
        .response("Earthquakes are caused by tectonic plate movement and volcanic activity.")
        .reference("Earthquakes are caused by movement of tectonic plates. " +
                "Plates suddenly shift and release energy. This creates seismic waves.")
        .retrievedContexts(List.of(
                "Tectonic plates are large sections of Earth's crust that move slowly.",
                "When tectonic plates collide, they can cause earthquakes.",
                "Seismic waves are energy released during earthquakes.",
                "Best time to visit Japan is during cherry blossom season."))
        .build();

NoiseSensitivityMetric.NoiseSensitivityConfig config =
        NoiseSensitivityMetric.NoiseSensitivityConfig.builder()
                .mode(NoiseSensitivityMetric.NoiseSensitivityMode.RELEVANT)
                .build();

NoiseSensitivityMetric metric = new NoiseSensitivityMetric(chatClient);
Double score = metric.singleTurnScore(config, sample);
// Result: ~0.1 (low sensitivity, good robustness)
```

### Configuration Parameters

| Parameter |         Type         | Required | Default  |              Description               |
|-----------|----------------------|----------|----------|----------------------------------------|
| `mode`    | NoiseSensitivityMode | No       | RELEVANT | RELEVANT or IRRELEVANT evaluation mode |

**Evaluation Modes:**

- **RELEVANT**: Measures errors from relevant retrieved contexts
- **IRRELEVANT**: Measures errors from irrelevant retrieved contexts

### How the Metric Works

1. **Statement Decomposition**: Breaks reference and response into atomic statements
2. **Faithfulness Evaluation**: Evaluates statements against contexts and reference
3. **Relevance Classification**: Determines context relevance based on reference
4. **Error Analysis**: Identifies incorrect statements attributable to context type
5. **Sensitivity Calculation**: Computes proportion of context-influenced errors

### Result Interpretation

**Lower scores are better for this metric:**

- **0.0-0.1**: Excellent robustness, minimal sensitivity to noise
- **0.1-0.3**: Good robustness, occasional sensitivity issues
- **0.3-0.5**: Moderate robustness, noticeable sensitivity problems
- **0.5-0.7**: Poor robustness, significant sensitivity issues
- **0.7-1.0**: Very poor robustness, highly sensitive to noise

---

## ResponseRelevancy

`ResponseRelevancy` measures how relevant a system's response is to the user's input. This metric evaluates how well the answer matches the user's question, considering response completeness, absence of redundant information, and presence of noncommittal (evasive) statements.

### When to Apply

**Ideal Scenarios:**
- **Chatbot quality evaluation**: Ensuring responses actually answer user questions
- **Question-answering systems (Q&A)**: Measuring answer relevance for factual queries
- **Dialogue systems**: Evaluating quality of multi-turn conversations
- **Search systems with answer generation**: Verifying generated answers match search queries
- **Virtual assistants**: Quality control for responses to user commands and questions
- **Educational platforms**: Assessing relevance of answers to learning questions

**Advantages:**
- Detects noncommittal answers ("I don't know", "Not sure")
- Identifies incomplete answers that only address part of the question
- Detects answers with excessive irrelevant information
- Works without reference answers
- Language-agnostic approach (works with any language)
- Uses semantic similarity for more accurate evaluation

**Limitations:**
- Requires embedding model for semantic similarity computation
- May require multiple LLM calls for question generation
- Evaluates relevance but not factual correctness
- May be sensitive to quality of LLM-generated questions

### Usage Example

```java
import ai.qa.solutions.metrics.retrieval.ResponseRelevancyMetric;
import ai.qa.solutions.sample.Sample;

// Example 1: Complete relevant answer
Sample completeSample = Sample.builder()
        .userInput("Where is France located and what is its capital?")
        .response("France is located in Western Europe, and its capital is Paris.")
        .build();

ResponseRelevancyMetric.ResponseRelevancyConfig config = 
        ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

ResponseRelevancyMetric metric = new ResponseRelevancyMetric(chatClient, embeddingModel);
Double score = metric.singleTurnScore(config, completeSample);
// Result: ~0.95 (high relevance, complete answer)

// Example 2: Incomplete answer
Sample incompleteSample = Sample.builder()
        .userInput("Where is France located and what is its capital?")
        .response("France is located in Western Europe.")
        .build();

Double incompleteScore = metric.singleTurnScore(config, incompleteSample);
// Result: ~0.65 (moderate relevance, only partially answers)

// Example 3: Noncommittal answer
Sample noncommittalSample = Sample.builder()
        .userInput("What is the capital of France?")
        .response("I don't know what the capital of France is.")
        .build();

Double noncommittalScore = metric.singleTurnScore(config, noncommittalSample);
// Result: 0.0 (zero relevance, noncommittal answer)
```

### Configuration Parameters

|      Parameter      | Type | Required | Default |                    Description                     |
|---------------------|------|----------|---------|----------------------------------------------------|
| `numberOfQuestions` | int  | No       | 3       | Number of questions to generate from answer (1-10) |

### How the Metric Works

1. **Question Generation**: LLM generates N artificial questions based on the response (considering original question context)
2. **Noncommittal Detection**: For each generated question, determines if the answer is noncommittal
3. **Noncommittal Check**: If all questions indicate noncommittal answer, returns score 0.0
4. **Embedding Computation**: Gets vector representations of original question and generated questions
5. **Similarity Calculation**: Computes cosine similarity between original question embedding and each generated question
6. **Aggregation**: Returns average of all similarity scores as final relevance score

**Key Metric Idea:**
- If answer is relevant to question, generated questions will be semantically similar to original question
- If answer is off-topic or incomplete, generated questions will differ from original
- Noncommittal answers like "I don't know" automatically receive 0.0 score

### Result Interpretation

- **0.9-1.0**: Excellent relevance, answer fully matches question
- **0.7-0.9**: Good relevance, answer mostly matches question
- **0.5-0.7**: Moderate relevance, answer partially matches or contains additional information
- **0.3-0.5**: Low relevance, answer weakly related to question or contains much irrelevant information
- **0.0-0.3**: Very low relevance, answer is off-topic or noncommittal

### Example Scenarios

**Scenario 1: Complete relevant answer**

```
Question: "What is photosynthesis?"
Answer: "Photosynthesis is the process by which plants convert light energy into 
         chemical energy. Plants use sunlight, water, and carbon dioxide to produce 
         glucose and oxygen."
Score: ~0.92 (excellent relevance)
```

**Scenario 2: Incomplete answer**

```
Question: "Who invented the light bulb and when?"
Answer: "Thomas Edison invented the light bulb."
Score: ~0.65 (moderate relevance, missing date information)
```

**Scenario 3: Off-topic answer**

```
Question: "What is the capital of France?"
Answer: "The Great Wall of China was built over 2000 years ago."
Score: ~0.15 (very low relevance, completely different topic)
```

**Scenario 4: Answer with redundant information**

```
Question: "What is the capital of France?"
Answer: "The capital of France is Paris. By the way, yesterday I went to the store 
         and bought milk. The weather was great, the sun was shining."
Score: ~0.55 (reduced relevance due to irrelevant information)
```

**Scenario 5: Noncommittal answer**

```
Question: "When was the light bulb invented?"
Answer: "I'm not sure when the light bulb was invented."
Score: 0.0 (noncommittal answer)
```

---

## Choosing the Right Metric

|            Use Case             | Recommended Metric  |                    Why                    |
|---------------------------------|---------------------|-------------------------------------------|
| Entity-focused applications     | ContextEntityRecall | Measures coverage of factual entities     |
| Retrieval ranking optimization  | ContextPrecision    | Evaluates ranking quality of contexts     |
| Information completeness        | ContextRecall       | Measures support for reference statements |
| Hallucination detection         | Faithfulness        | Identifies unsupported claims             |
| System robustness testing       | NoiseSensitivity    | Measures sensitivity to noisy inputs      |
| Response relevance evaluation   | ResponseRelevancy   | Measures answer-question alignment        |
| Tourism/travel systems          | ContextEntityRecall | Ensures location/date coverage            |
| Academic knowledge systems      | ContextRecall       | Verifies comprehensive information        |
| News/information systems        | Faithfulness        | Maintains factual accuracy                |
| Chatbots and virtual assistants | ResponseRelevancy   | Ensures dialogue relevance                |

---

## Best Practices

### 1. Metric Selection Strategy

**For RAG evaluation pipeline:**

```java
// Stage 1: Basic retrieval quality
ContextPrecision precision = new ContextPrecision(chatClient);

// Stage 2: Information completeness
ContextRecall recall = new ContextRecall(chatClient);

// Stage 3: Response quality
Faithfulness faithfulness = new Faithfulness(chatClient);

// Stage 4: Robustness testing
NoiseSensitivity sensitivity = new NoiseSensitivity(chatClient);
```

### 2. Sample Preparation

**Essential fields for retrieval metrics:**

```java
Sample sample = Sample.builder()
        .userInput("Required for all metrics")
        .response("Required for Faithfulness and NoiseSensitivity")
        .reference("Required for ContextRecall and NoiseSensitivity")
        .retrievedContexts(List.of("Required for all retrieval metrics"))
        .build();
```

### 3. Parallel Evaluation

```java
// Evaluate multiple aspects simultaneously
CompletableFuture<Double> precisionFuture =
        precisionMetric.singleTurnScoreAsync(precisionConfig, sample);
CompletableFuture<Double> recallFuture =
        recallMetric.singleTurnScoreAsync(recallConfig, sample);
CompletableFuture<Double> faithfulnessFuture =
        faithfulnessMetric.singleTurnScoreAsync(sample);

// Wait for all results
CompletableFuture.allOf(precisionFuture, recallFuture, faithfulnessFuture).join();
```

### 4. Context Optimization

**For better metric performance:**

- Ensure retrieved contexts are relevant and well-formatted
- Maintain consistent entity naming across contexts
- Provide sufficient context diversity for comprehensive evaluation
- Remove duplicate or near-duplicate contexts

### 5. Reference Quality

**For ContextRecall and NoiseSensitivity:**

- Write clear, factual reference answers
- Break complex information into clear statements
- Ensure references contain verifiable claims
- Avoid subjective or opinion-based content

---

## Troubleshooting

### Issue: Low ContextEntityRecall despite good contexts

**Potential Causes:**

- Entity extraction differences between reference and contexts
- Inconsistent entity naming or formatting
- Missing key entities in retrieved contexts

**Solutions:**

- Standardize entity naming across texts
- Verify entity extraction is working correctly
- Check if important entities are present in contexts

### Issue: Inconsistent ContextPrecision scores

**Potential Causes:**

- Ambiguous relevance criteria
- LLM evaluation inconsistency
- Mixed quality in retrieved contexts

**Solutions:**

- Use reference-based evaluation when possible
- Ensure clear, specific user queries
- Improve context quality and relevance

### Issue: ContextRecall too strict/lenient

**Potential Causes:**

- Statement decomposition creating too fine/coarse granularity
- Attribution criteria too strict/lenient
- LLM interpretation variations

**Solutions:**

- Review statement decomposition quality
- Adjust reference answer complexity
- Test with multiple examples to verify consistency

### Issue: Faithfulness false positives/negatives

**Potential Causes:**

- Context contains contradictory information
- Statement decomposition issues
- LLM factual reasoning limitations

**Solutions:**

- Ensure contexts are factually consistent
- Review generated statements for quality
- Consider using multiple LLM evaluations

### Issue: NoiseSensitivity unexpected results

**Potential Causes:**

- Complex evaluation logic
- Mode selection (RELEVANT vs IRRELEVANT)
- Insufficient context diversity

**Solutions:**

- Verify evaluation mode matches use case
- Ensure good mix of relevant/irrelevant contexts
- Check reference and response consistency

### Issue: Low ResponseRelevancy scores for correct answers

**Potential Causes:**

- Answer contains redundant information beyond the main point
- Answer style significantly differs from question style
- Quality of LLM-generated questions insufficient
- Embedding model doesn't capture semantic similarity well

**Solutions:**

- Ensure answers focus on requested information
- Check quality of generated questions in logs
- Try increasing numberOfQuestions for more stable scores
- Consider using a higher-quality embedding model

### Issue: ResponseRelevancy doesn't detect noncommittal answers

**Potential Causes:**

- Noncommittal nature expressed implicitly or in soft form
- LLM incorrectly classifies answer as committal
- Answer contains mixed content (partly noncommittal, partly not)

**Solutions:**

- Check noncommittal values in generated questions
- Ensure using current version of generation prompt
- For mixed answers, expect intermediate scores rather than 0.0

### Issue: Unstable ResponseRelevancy scores

**Potential Causes:**

- Low number of generated questions (numberOfQuestions)
- Variability in LLM question generation
- Unclear or ambiguous user questions

**Solutions:**

- Increase numberOfQuestions to 5-7 for more stable scores
- Test on multiple examples to assess overall consistency
- Clarify user questions for more unambiguous interpretation

---

## Advanced Examples

### Example 1: Complete RAG Evaluation Pipeline

```java
public class RAGEvaluationPipeline {

    public RAGEvaluationResult evaluateRAGSystem(Sample sample) {
        // 1. Entity coverage evaluation
        ContextEntityRecallMetric entityMetric = new ContextEntityRecallMetric(chatClient);
        Double entityScore = entityMetric.singleTurnScore(
                ContextEntityRecallMetric.ContextEntityRecallConfig.builder().build(), sample);

        // 2. Retrieval precision evaluation
        ContextPrecisionMetric precisionMetric = new ContextPrecisionMetric(chatClient);
        Double precisionScore = precisionMetric.singleTurnScore(
                ContextPrecisionMetric.ContextPrecisionConfig.builder()
                        .evaluationStrategy(ContextPrecisionMetric.EvaluationStrategy.REFERENCE_BASED)
                        .build(), sample);

        // 3. Information completeness evaluation
        ContextRecallMetric recallMetric = new ContextRecallMetric(chatClient);
        Double recallScore = recallMetric.singleTurnScore(
                ContextRecallMetric.ContextRecallConfig.builder().build(), sample);

        // 4. Response faithfulness evaluation
        FaithfulnessMetric faithfulnessMetric = new FaithfulnessMetric(chatClient);
        Double faithfulnessScore = faithfulnessMetric.singleTurnScore(sample);

        // 5. Noise sensitivity evaluation
        NoiseSensitivityMetric sensitivityMetric = new NoiseSensitivityMetric(chatClient);
        Double sensitivityScore = sensitivityMetric.singleTurnScore(
                NoiseSensitivityMetric.NoiseSensitivityConfig.builder()
                        .mode(NoiseSensitivityMetric.NoiseSensitivityMode.RELEVANT)
                        .build(), sample);

        // 6. Response relevance evaluation
        ResponseRelevancyMetric relevancyMetric = new ResponseRelevancyMetric(chatClient, embeddingModel);
        Double relevancyScore = relevancyMetric.singleTurnScore(
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig(), sample);

        return new RAGEvaluationResult(
                entityScore, precisionScore, recallScore,
                faithfulnessScore, sensitivityScore, relevancyScore);
    }
}
```

### Example 2: Retrieval Strategy Comparison

```java
public class RetrievalComparison {

    public void compareRetrievalStrategies() {
        String query = "What is machine learning?";
        String reference = "Machine learning is a branch of AI that uses algorithms to learn from data.";

        // Strategy A: Keyword-based retrieval
        List<String> keywordContexts = List.of(
                "Machine learning algorithms learn patterns from data.",
                "AI encompasses various techniques including ML.",
                "Data science uses statistical methods for analysis."
        );

        // Strategy B: Semantic retrieval
        List<String> semanticContexts = List.of(
                "Machine learning is a subset of artificial intelligence focused on data-driven learning.",
                "ML algorithms automatically improve performance through experience.",
                "Supervised and unsupervised learning are main ML paradigms."
        );

        Sample sampleA = Sample.builder()
                .userInput(query).reference(reference)
                .retrievedContexts(keywordContexts).build();

        Sample sampleB = Sample.builder()
                .userInput(query).reference(reference)
                .retrievedContexts(semanticContexts).build();

        // Compare using multiple metrics
        ContextRecallMetric recallMetric = new ContextRecallMetric(chatClient);
        ContextPrecisionMetric precisionMetric = new ContextPrecisionMetric(chatClient);

        // Evaluate both strategies
        Map<String, Double> strategyAScores = Map.of(
                "recall", recallMetric.singleTurnScore(
                        ContextRecallMetric.ContextRecallConfig.builder().build(), sampleA),
                "precision", precisionMetric.singleTurnScore(
                        ContextPrecisionMetric.ContextPrecisionConfig.builder().build(), sampleA)
        );

        Map<String, Double> strategyBScores = Map.of(
                "recall", recallMetric.singleTurnScore(
                        ContextRecallMetric.ContextRecallConfig.builder().build(), sampleB),
                "precision", precisionMetric.singleTurnScore(
                        ContextPrecisionMetric.ContextPrecisionConfig.builder().build(), sampleB)
        );

        System.out.println("Keyword-based strategy: " + strategyAScores);
        System.out.println("Semantic-based strategy: " + strategyBScores);
    }
}
```

### Example 3: Domain-Specific Evaluation

```java
public class DomainSpecificEvaluation {

    // Tourism domain evaluation
    public void evaluateTourismRAG() {
        Sample tourismSample = Sample.builder()
                .userInput("Tell me about visiting the Louvre Museum")
                .reference("The Louvre Museum is located in Paris, France. " +
                        "It houses the Mona Lisa painted by Leonardo da Vinci. " +
                        "The museum is open Tuesday through Sunday.")
                .retrievedContexts(List.of(
                        "The Louvre Museum is one of the world's largest art museums located in Paris.",
                        "Leonardo da Vinci's Mona Lisa is the most famous painting in the Louvre.",
                        "The museum is closed on Mondays and open Tuesday through Sunday.",
                        "Advance booking is recommended during peak tourist season."))
                .build();

        // Focus on entity coverage for tourism
        ContextEntityRecallMetric entityMetric = new ContextEntityRecallMetric(chatClient);
        Double entityScore = entityMetric.singleTurnScore(
                ContextEntityRecallMetric.ContextEntityRecallConfig.builder().build(),
                tourismSample);

        System.out.println("Tourism entity coverage: " + entityScore);
    }
}
```

### Example 4: Chatbot Response Quality Evaluation

```java
public class ChatbotEvaluation {
    
    // Evaluate chatbot response relevance
    public void evaluateChatbotResponses() {
        ResponseRelevancyMetric relevancyMetric = new ResponseRelevancyMetric(chatClient, embeddingModel);
        
        // Test 1: Complete relevant answer
        Sample fullAnswerSample = Sample.builder()
            .userInput("What are your store hours?")
            .response("Our store is open Monday through Friday from 9:00 AM to 6:00 PM, " +
                     "Saturday from 10:00 AM to 4:00 PM, and closed on Sunday.")
            .build();
        
        Double fullScore = relevancyMetric.singleTurnScore(
            ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig(), 
            fullAnswerSample);
        System.out.println("Full answer: " + fullScore); // Expected: ~0.90-0.95
        
        // Test 2: Incomplete answer
        Sample incompleteAnswerSample = Sample.builder()
            .userInput("What are your store hours?")
            .response("Our store is open Monday through Friday from 9:00 AM to 6:00 PM.")
            .build();
        
        Double incompleteScore = relevancyMetric.singleTurnScore(
            ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig(), 
            incompleteAnswerSample);
        System.out.println("Incomplete answer: " + incompleteScore); // Expected: ~0.65-0.75
        
        // Test 3: Noncommittal answer
        Sample noncommittalSample = Sample.builder()
            .userInput("What are your store hours?")
            .response("I'm not sure about our exact store hours.")
            .build();
        
        Double noncommittalScore = relevancyMetric.singleTurnScore(
            ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig(), 
            noncommittalSample);
        System.out.println("Noncommittal answer: " + noncommittalScore); // Expected: 0.0
        
        // Test 4: Answer with redundant information
        Sample redundantSample = Sample.builder()
            .userInput("What are your store hours?")
            .response("Our store is open from 9:00 AM to 6:00 PM. " +
                     "By the way, we're having a sale right now. " +
                     "The weather is great today, isn't it?")
            .build();
        
        Double redundantScore = relevancyMetric.singleTurnScore(
            ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig(), 
            redundantSample);
        System.out.println("Answer with redundant info: " + redundantScore); // Expected: ~0.50-0.65
        
        // Analyze results
        if (fullScore > 0.85 && noncommittalScore < 0.1) {
            System.out.println("✓ Chatbot correctly answers direct questions");
        }
        if (incompleteScore < fullScore) {
            System.out.println("✓ Metric correctly identifies incomplete answers");
        }
    }
    
    // Evaluation with custom configuration
    public void evaluateWithCustomConfig() {
        ResponseRelevancyMetric relevancyMetric = new ResponseRelevancyMetric(chatClient, embeddingModel);
        
        Sample sample = Sample.builder()
            .userInput("Explain what machine learning is")
            .response("Machine learning is a branch of artificial intelligence " +
                     "that enables computers to learn from data without explicit programming. " +
                     "ML algorithms identify patterns in data and use them for prediction.")
            .build();
        
        // Use more questions for more stable evaluation
        ResponseRelevancyMetric.ResponseRelevancyConfig config = 
            ResponseRelevancyMetric.ResponseRelevancyConfig.builder()
                .numberOfQuestions(5)
                .build();
        
        Double score = relevancyMetric.singleTurnScore(config, sample);
        System.out.println("Relevance score (5 questions): " + score);
    }
}
```

