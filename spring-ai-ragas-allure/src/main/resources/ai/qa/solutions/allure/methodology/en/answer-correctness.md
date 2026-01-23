# Answer Correctness Metric

## Overview

Answer Correctness is a composite metric that evaluates how well an AI response matches a reference answer by combining two complementary approaches:

1. **Factual Correctness (75% default weight)** - Verifies that specific facts are correct using claims decomposition and NLI
2. **Semantic Similarity (25% default weight)** - Ensures the overall meaning is preserved using embedding similarity

This dual approach provides a comprehensive assessment that catches both specific factual errors and overall meaning drift.

## Glossary

|        Term         |                      Definition                       |
|---------------------|-------------------------------------------------------|
| Response            | The AI-generated answer to evaluate                   |
| Reference           | The ground truth or expected answer                   |
| Factual Correctness | Score measuring accuracy of individual facts via NLI  |
| Semantic Similarity | Score measuring meaning preservation via embeddings   |
| Weighted Average    | Combination of both scores using configurable weights |

## Algorithm

### Step 1: Compute Factual Correctness

The FactualCorrectnessMetric is invoked to:
1. Decompose response into atomic claims
2. Decompose reference into atomic claims
3. Verify claims using Natural Language Inference
4. Calculate F1/Precision/Recall score

### Step 2: Compute Semantic Similarity

The SemanticSimilarityMetric is invoked to:
1. Generate embeddings for response and reference
2. Calculate cosine similarity between vectors

### Step 3: Combine Scores

Compute weighted average:

```
score = factualWeight × factualScore + semanticWeight × semanticScore
```

## Formula

**Combined Score:**

```
AnswerCorrectness = w_f × FactualCorrectness + w_s × SemanticSimilarity
```

Where:
- `w_f` = factual weight (default 0.75)
- `w_s` = semantic weight (default 0.25)
- `w_f + w_s = 1.0`

## Configuration

```java
AnswerCorrectnessMetric.AnswerCorrectnessConfig config =
    AnswerCorrectnessMetric.AnswerCorrectnessConfig.builder()
        .factualWeight(0.75)  // Weight for factual correctness
        .semanticWeight(0.25) // Weight for semantic similarity
        .models(List.of("model-1", "model-2"))
        .build();
```

### Preset Configurations

|       Preset        | Factual Weight | Semantic Weight |           Use Case            |
|---------------------|----------------|-----------------|-------------------------------|
| `defaultConfig()`   | 75%            | 25%             | General purpose               |
| `equalWeights()`    | 50%            | 50%             | Balanced evaluation           |
| `factualFocused()`  | 90%            | 10%             | Fact-critical applications    |
| `semanticFocused()` | 10%            | 90%             | Meaning-critical applications |

## Score Interpretation

| Score Range |   Level   |                 Interpretation                  |
|-------------|-----------|-------------------------------------------------|
| 0.9 - 1.0   | Excellent | Both factually correct and semantically aligned |
| 0.7 - 0.9   | Good      | Mostly correct with good similarity             |
| 0.5 - 0.7   | Moderate  | Some issues with facts or meaning               |
| 0.0 - 0.5   | Poor      | Significant factual errors or semantic mismatch |

## Usage Example

```java
Sample sample = Sample.builder()
    .response("Paris is the capital of France. The Eiffel Tower is in Paris.")
    .reference("Paris is the capital of France. The Eiffel Tower is located in Paris.")
    .build();

Double score = answerCorrectnessMetric.singleTurnScore(sample);
// Returns: ~0.95 (high factual match + high semantic similarity)
```

## When to Use

**Good for:**
- Comprehensive answer quality evaluation
- Detecting both factual errors and meaning drift
- Applications requiring both precision and semantic understanding

**Consider alternatives when:**
- Only factual accuracy matters → Use FactualCorrectnessMetric
- Only meaning preservation matters → Use SemanticSimilarityMetric
- Detailed claim-level analysis needed → Use FactualCorrectnessMetric directly

## Dependencies

This metric internally uses:
- `FactualCorrectnessMetric` for factual verification
- `SemanticSimilarityMetric` for embedding similarity

Both must be available in the Spring context.
