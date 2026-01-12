# Aspect Critic Metric

## Glossary

- **Aspect** — a user-defined criterion for evaluating the response
- **Agentic System** — an LLM-based system that generates responses
- **Strictness** — number of LLM iterations per model for self-consistency via majority voting

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
2. **Evaluation with Strictness** — for each model, LLM is called `strictness` times (default: 1)
3. **Majority Voting per Model** — if strictness > 1, the final verdict for each model is determined by majority vote across iterations
4. **Model Score Conversion** — each model's verdict is converted: PASS → 1.0, FAIL → 0.0
5. **Aggregation** — final score is computed by averaging all model scores

### Strictness Parameter

The `strictness` parameter enables self-consistency checks through multiple LLM iterations:

- **strictness = 1** (default): Single evaluation per model
- **strictness = 3**: Three evaluations per model, majority vote determines result
- **strictness = 5**: Five evaluations, more robust but slower

Example with strictness = 3 and 2 models:

```
Model A: [PASS, PASS, FAIL] → majority PASS → 1.0
Model B: [FAIL, FAIL, PASS] → majority FAIL → 0.0
Final Score: (1.0 + 0.0) / 2 = 0.5 = 50%
```

## Typical Aspects for Evaluation

- **Harmlessness** — absence of harmful or offensive content
- **Conciseness** — brevity and absence of redundancy
- **Coherence** — logical structure
- **Completeness** — coverage of all required points
- **Factuality** — absence of speculation and assumptions

## Formula

### Single Model

```
model_verdict = majority_vote(iterations) if strictness > 1 else single_evaluation
model_score = 1.0 if model_verdict == PASS else 0.0
```

### Multi-Model Aggregation

```
final_score = average(model_scores)
```

Example: 2 PASS + 3 FAIL = 2/5 = 0.40 = 40%

## References

- [RAGAS Documentation](https://github.com/vibrantlabsai/ragas/blob/main/docs/concepts/metrics/available_metrics/general_purpose.md)

