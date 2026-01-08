# Aspect Critic Metric

## Glossary

- **Aspect** — a user-defined criterion for evaluating the response
- **Agentic System** — an LLM-based system that generates responses

## Description

The metric performs a binary check of the agentic system's response against a defined criterion. The evaluation result is either pass (1.0) or fail (0.0).

## Example

**Criterion:** "The response should not contain harmful or offensive content."

**Agentic System Response:** "The Eiffel Tower was built in 1889 in Paris, France."

**Result:** No harmful content present → **1.0 (pass)**

---

**Criterion:** "The response must include a specific date or year."

**Agentic System Response:** "The Eiffel Tower is located in Paris and is very tall."

**Result:** No date present → **0.0 (fail)**

## Score Interpretation

| Score |               Meaning                |
|-------|--------------------------------------|
| 1.0   | Response meets the criterion         |
| 0.0   | Response does not meet the criterion |

The metric is binary — only two results are possible.

## Algorithm

1. **Criterion Definition** — the aspect for evaluation is specified
2. **Evaluation** — LLM analyzes the response against the criterion
3. **Verdict** — result is returned with reasoning

## Typical Aspects for Evaluation

- **Harmlessness** — absence of harmful or offensive content
- **Conciseness** — brevity and absence of redundancy
- **Coherence** — logical structure
- **Completeness** — coverage of all required points
- **Factuality** — absence of speculation and assumptions

## Formula

```
score = 1.0 if criterion is met
score = 0.0 if criterion is not met
```

## References

- [RAGAS Documentation](https://github.com/vibrantlabsai/ragas/blob/main/docs/concepts/metrics/available_metrics/general_purpose.md)

