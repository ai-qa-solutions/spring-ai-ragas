# Simple Criteria Score Metric

## Glossary

- **Criterion** — a user-defined rule for evaluating response quality
- **Agentic System** — an LLM-based system that generates responses

## Description

The metric evaluates the agentic system's response against a defined criterion on a continuous scale from 0.0 to 1.0. Unlike the binary Aspect Critic metric, it provides a graduated score.

## Example

**Criterion:** "Technical accuracy of the response"

**Agentic System Response:** "The Eiffel Tower was built in 1889. It is approximately 330 meters tall. It was designed by Gustave Eiffel."

**Analysis:**
- Year is correct (1889) ✓
- Height is correct (330m) ✓
- Designer is correct ✓

**Score:** **0.95 (95%)**

---

**Agentic System Response:** "The Eiffel Tower was built around 1900. It is the tallest structure in Paris."

**Analysis:**
- Year is incorrect (1889, not 1900) ✗
- Statement about height is debatable ⚠

**Score:** **0.40 (40%)**

## Score Interpretation

|  Score  |                  Meaning                  |
|---------|-------------------------------------------|
| 90-100% | Full compliance with the criterion        |
| 70-90%  | Predominant compliance with the criterion |
| 50-70%  | Partial compliance with the criterion     |
| 0-50%   | Non-compliance with the criterion         |

## Algorithm

1. **Criterion Definition** — the evaluation rule is specified
2. **Analysis** — LLM evaluates the response against the criterion
3. **Score Assignment** — a score from 0.0 to 1.0 is returned with reasoning

## Example Criteria

- **Technical Accuracy** — factual correctness of information
- **Helpfulness** — practical value for the user
- **Clarity** — comprehensibility and structure of presentation
- **Completeness** — degree of topic coverage

## Formula

```
score = LLM_assigned_score (from 0.0 to 1.0)
```

LLM assigns a continuous score based on analysis of criterion compliance.

## References

- [RAGAS Documentation](https://github.com/vibrantlabsai/ragas/blob/main/docs/concepts/metrics/available_metrics/general_purpose.md)

