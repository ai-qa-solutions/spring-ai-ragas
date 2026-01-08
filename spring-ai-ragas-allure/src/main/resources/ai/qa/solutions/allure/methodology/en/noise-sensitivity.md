# Noise Sensitivity Metric

## Glossary

- **Noise** — irrelevant contexts not related to the user's question
- **Reference (Ground Truth)** — the expected correct answer to the user's question
- **Context** — information fragments provided to the agentic system
- **Agentic System** — an LLM-based system that generates responses

## Description

The metric evaluates the agentic system's resistance to irrelevant information (noise) in the context. It checks whether the system used noisy contexts when forming the response. A low score indicates high resistance — the agentic system successfully ignores irrelevant information.

**Note:** This is an inverse metric — the lower the score, the better.

## Example

**User Question:** "When was the Eiffel Tower built?"

**Reference (Ground Truth):** "The Eiffel Tower was built in 1889."

**Contexts:**
1. "The Eiffel Tower was completed in 1889." (relevant)
2. "The weather in Paris is mild in spring." (noise)
3. "The tower has 1,665 steps." (noise)

**Agentic System Response:** "The Eiffel Tower was built in 1889. Paris has mild weather in spring."

**Analysis:**
- Fact about 1889 — from relevant context ✓
- Fact about weather — from noise context ✗

**Score:** > 0% (noise used — bad)

If the response contained only the fact about 1889, the score would be **0% (good)**.

## Score Interpretation

|  Score  |                       Meaning                        |
|---------|------------------------------------------------------|
| 0-10%   | Agentic system successfully ignores noise            |
| 10-30%  | Agentic system is mostly resistant to noise          |
| 30-50%  | Agentic system sometimes uses noisy information      |
| 50-100% | Agentic system is susceptible to irrelevant contexts |

## Algorithm

1. **Statement Extraction** — reference and response are broken into atomic statements
2. **Reference Comparison** — response statements are checked against the reference
3. **Source Analysis** — determination of which contexts statements came from
4. **Score Calculation** — proportion of noisy information in the response is evaluated

## Modes

- **RELEVANT** — measures errors from relevant contexts
- **IRRELEVANT** — measures errors from irrelevant (noise) contexts

## Formula

```
score = noise_statements / total_statements
```

Where:
- `noise_statements` — number of statements in response taken from noise contexts
- `total_statements` — total number of statements in the response

## References

- [RAGAS Documentation](https://github.com/vibrantlabsai/ragas/blob/main/docs/concepts/metrics/available_metrics/noise_sensitivity.md)

