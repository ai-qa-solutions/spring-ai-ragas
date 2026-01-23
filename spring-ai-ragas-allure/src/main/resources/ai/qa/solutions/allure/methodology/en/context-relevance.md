# Context Relevance Metric (NVIDIA-style)

## Glossary

- **Context** — a text chunk retrieved from a knowledge base or document store
- **User Question** — the input query from the user
- **Relevance Score** — evaluation of how well a context helps answer the question
- **Raw Score** — score on 0-2 scale before normalization
- **Normalized Score** — score on 0-1 scale after dividing raw score by 2

## Description

The metric evaluates whether retrieved contexts are relevant to the user's question. It uses a 0-2 scoring scale that is then normalized to 0-1.

This is an NVIDIA-style metric that provides granular relevance assessment:

- **0** — Not relevant: Context does not contain information to answer the question
- **1** — Partially relevant: Context contains some relevant information but may be incomplete
- **2** — Fully relevant: Context contains comprehensive information to answer the question

## Example

**User Question:** "What is machine learning and how does it work?"

**Retrieved Contexts:**

1. "Machine learning is a subset of artificial intelligence that enables systems to automatically learn and improve from experience without being explicitly programmed."

2. "The weather forecast shows partly cloudy skies tomorrow."

**Analysis:**

- Context 1: Raw score = 2 (fully relevant), Normalized = 1.0
- Context 2: Raw score = 0 (not relevant), Normalized = 0.0
- Average: (1.0 + 0.0) / 2 = 0.5

**Score:** **0.5 (50%)** — Mixed Relevance

## Score Interpretation

|  Score  |                      Meaning                      |
|---------|---------------------------------------------------|
| 90-100% | Excellent — all contexts are highly relevant      |
| 70-89%  | Good — most contexts contain relevant information |
| 50-69%  | Moderate — mixed relevance across contexts        |
| 0-49%   | Poor — contexts are mostly irrelevant             |

## Algorithm

1. **Evaluate Each Context** — LLM scores each context on 0-2 scale
2. **Normalize Scores** — Divide each raw score by 2 to get 0-1 range
3. **Average Scores** — Compute mean across all context evaluations

### Score Calculation

```
For each context:
  normalized_score = raw_score / 2.0

Final Score = average(normalized_scores)
```

## Formula

```
Context Relevance = (1/N) * sum(score_i / 2)

where:
  N = number of contexts
  score_i = raw score (0-2) for context i
```

## Configuration

```java
ContextRelevanceConfig config = ContextRelevanceConfig.builder()
    .temperature(0.1)  // Lower temperature for consistent results
    .build();
```

|   Parameter   |   Type   | Default |           Description           |
|---------------|----------|---------|---------------------------------|
| `temperature` | `double` | `0.1`   | LLM temperature for evaluation  |
| `models`      | `List`   | all     | Model IDs to use for evaluation |

## Sample Requirements

|        Field        | Required |           Description            |
|---------------------|----------|----------------------------------|
| `userInput`         | Yes      | The user's question              |
| `retrievedContexts` | Yes      | List of retrieved context chunks |

## Use Cases

- Evaluating RAG retrieval quality
- Testing document retrieval systems
- Assessing search result relevance
- Validating knowledge base retrieval

## References

- [NVIDIA RAG Evaluation](https://developer.nvidia.com/blog/evaluating-retrieval-augmented-generation-pipelines/)
- [RAGAS Documentation](https://docs.ragas.io/)

