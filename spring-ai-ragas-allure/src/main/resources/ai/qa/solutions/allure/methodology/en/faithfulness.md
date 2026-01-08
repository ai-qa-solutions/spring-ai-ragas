# Faithfulness Metric

## Glossary

- **Context** — source data provided to the agentic system for generating a response
- **Hallucination** — a statement in the response not supported by the context
- **Agentic System** — an LLM-based system that generates responses

## Description

The metric evaluates factual consistency of the agentic system's response with the provided context. The response is broken down into individual statements, each of which is verified against the context.

## Example

**Context:** "The Eiffel Tower was built in 1889 in Paris."

**Agentic System Response:** "The Eiffel Tower was built in 1889. It is the tallest tower in the world."

**Statement Analysis:**
1. "Built in 1889" → verified by context ✓
2. "Tallest tower in the world" → not verified by context (hallucination) ✗

**Score:** 1/2 = **0.50 (50%)**

## Score Interpretation

|  Score  |                              Meaning                               |
|---------|--------------------------------------------------------------------|
| 90-100% | High faithfulness — all statements verified by context             |
| 70-90%  | Good faithfulness — most statements verified                       |
| 50-70%  | Moderate faithfulness — unverified statements present              |
| 0-50%   | Low faithfulness — significant portion of information not verified |

## Algorithm

1. **Statement Extraction** — LLM breaks the response into atomic statements
2. **Verification** — each statement is checked against the context
3. **Score Calculation** — ratio of verified statements to total statements

## Formula

```
score = verified_statements / total_statements
```

Where:
- `verified_statements` — number of statements confirmed by context
- `total_statements` — total number of statements in the response

## References

- [RAGAS Documentation](https://github.com/vibrantlabsai/ragas/blob/main/docs/concepts/metrics/available_metrics/faithfulness.md)
- [Original RAGAS Paper](https://arxiv.org/abs/2309.15217)

