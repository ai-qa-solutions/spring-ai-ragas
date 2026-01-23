# Response Groundedness Metric (NVIDIA-style)

## Glossary

- **Response** — the AI-generated answer to be evaluated
- **Context** — the retrieved text chunks used to inform the response
- **Grounded** — information that can be found in or inferred from the context
- **Raw Score** — score on 0-2 scale before normalization
- **Normalized Score** — score on 0-1 scale after dividing raw score by 2
- **Heuristic Shortcuts** — quick checks to bypass LLM evaluation for obvious cases

## Description

The metric evaluates whether the AI response is grounded in (supported by) the retrieved contexts. It uses a 0-2 scoring scale that is then normalized to 0-1.

This is an NVIDIA-style metric that provides:

- **0** — Not grounded: Response contains significant information not found in context
- **1** — Partially grounded: Response is partially supported by context
- **2** — Fully grounded: Response is completely supported by the context

The metric supports optional heuristic shortcuts:
- Exact match: If response exactly matches a context → returns 1.0 immediately
- Contained: If response is contained within context → returns 1.0 immediately

## Example

**Response:** "Paris is the capital of France and has a population of about 2 million."

**Context:** "Paris is the capital and largest city of France. It is located along the Seine River."

**Analysis:**
- The response states Paris is the capital of France — grounded
- The response mentions population of 2 million — not grounded (not in context)
- Raw score: 1 (partially grounded)
- Normalized: 1/2 = 0.5

**Score:** **0.5 (50%)** — Partially Grounded

## Score Interpretation

|  Score  |                   Meaning                   |
|---------|---------------------------------------------|
| 90-100% | Excellent — response is completely grounded |
| 70-89%  | Good — response is mostly grounded          |
| 50-69%  | Moderate — response is partially grounded   |
| 0-49%   | Poor — response contains unsupported claims |

## Algorithm

1. **Apply Heuristics** (if enabled)
   - Check for exact match between response and context
   - Check if response is fully contained in context
   - If match found → return 1.0
2. **Evaluate Groundedness** — LLM scores response on 0-2 scale
3. **Normalize Score** — Divide raw score by 2

### Score Calculation

```
If heuristic match:
  Score = 1.0

Otherwise:
  Score = raw_score / 2.0
```

## Formula

```
Response Groundedness = raw_score / 2

where:
  raw_score = LLM evaluation (0, 1, or 2)
```

## Configuration

```java
ResponseGroundednessConfig config = ResponseGroundednessConfig.builder()
    .useHeuristicShortcuts(true)  // Enable quick checks (default: true)
    .temperature(0.1)              // LLM temperature (default: 0.1)
    .build();
```

|        Parameter        |   Type    | Default |           Description           |
|-------------------------|-----------|---------|---------------------------------|
| `useHeuristicShortcuts` | `boolean` | `true`  | Enable heuristic shortcuts      |
| `temperature`           | `double`  | `0.1`   | LLM temperature for evaluation  |
| `models`                | `List`    | all     | Model IDs to use for evaluation |

## Sample Requirements

|        Field        | Required |           Description            |
|---------------------|----------|----------------------------------|
| `response`          | Yes      | The AI response to evaluate      |
| `retrievedContexts` | Yes      | List of retrieved context chunks |

## Use Cases

- Evaluating RAG response quality
- Testing for hallucinations in AI responses
- Validating response faithfulness to source material
- Measuring grounding in document Q&A systems

## Comparison with Faithfulness

|       Aspect        |   ResponseGroundedness   |      Faithfulness       |
|---------------------|--------------------------|-------------------------|
| Scoring Scale       | 0-2 normalized to 0-1    | Binary per claim        |
| Granularity         | Overall response         | Per claim               |
| Heuristic Shortcuts | Yes (optional)           | No                      |
| LLM Calls           | 1 (or 0 with heuristics) | 2+ (decompose + verify) |

## References

- [NVIDIA RAG Evaluation](https://developer.nvidia.com/blog/evaluating-retrieval-augmented-generation-pipelines/)
- [RAGAS Documentation](https://docs.ragas.io/)

