# Rubrics Score Metric

## Glossary

- **Rubric** — an evaluation scale with multiple levels and clear criteria for each
- **Rubric Level** — one of the scores on the scale (e.g., 5 = Excellent, 4 = Good)
- **Normalization** — converting the level to a scale from 0 to 1
- **Agentic System** — an LLM-based system that generates responses

## Description

The metric evaluates the agentic system's response against a defined rubric with multiple levels. Each level has a clear description of compliance criteria. The LLM analyzes the response and selects the most appropriate level. The result is normalized to a scale from 0.0 to 1.0.

## Example

**Rubric (5-point scale):**

| Level |   Name    |                          Criteria                           |
|-------|-----------|-------------------------------------------------------------|
| 5     | Excellent | Comprehensive, accurate, well-structured, includes examples |
| 4     | Good      | Accurate with minor omissions, good structure               |
| 3     | Adequate  | Covers main points but lacks depth                          |
| 2     | Poor      | Significant gaps or inaccuracies                            |
| 1     | Very Poor | Largely incorrect or irrelevant                             |

**Agentic System Response:** "The Eiffel Tower was built in 1889 for the World's Fair. It stands 330 meters tall and was designed by Gustave Eiffel."

**Analysis:**
- Accurate information ✓
- Covers multiple aspects ✓
- Good structure ✓
- Lacks depth (no examples, no historical context)

**Selected Level:** 4 (Good)

**Score:** (4-1)/(5-1) = **0.75 (75%)**

## Score Interpretation

Score is normalized based on the rubric scale:

| Level (out of 5) |    Score    |  Meaning  |
|------------------|-------------|-----------|
| 5                | 1.00 (100%) | Excellent |
| 4                | 0.75 (75%)  | Good      |
| 3                | 0.50 (50%)  | Adequate  |
| 2                | 0.25 (25%)  | Poor      |
| 1                | 0.00 (0%)   | Very Poor |

## Algorithm

1. **Rubric Definition** — levels with clear criteria descriptions are specified
2. **Analysis** — LLM analyzes the response and compares it to each level
3. **Level Selection** — LLM selects the most appropriate level
4. **Normalization** — level is converted to a score from 0 to 1

## Formula

```
score = (selected_level - min_level) / (max_level - min_level)
```

Where:
- `selected_level` — rubric level selected by the LLM
- `min_level` — minimum level in the rubric (usually 1)
- `max_level` — maximum level in the rubric (usually 5)

## References

- [RAGAS Documentation](https://github.com/vibrantlabsai/ragas/blob/main/docs/concepts/metrics/available_metrics/general_purpose.md)

