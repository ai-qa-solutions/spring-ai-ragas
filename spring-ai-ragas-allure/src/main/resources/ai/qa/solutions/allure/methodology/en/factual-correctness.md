# Factual Correctness Metric

## Glossary

- **Claim** — an atomic, independently verifiable factual statement
- **NLI (Natural Language Inference)** — determining if a hypothesis is supported by, contradicted by, or neutral to a premise
- **Precision** — ratio of response claims supported by reference
- **Recall** — ratio of reference claims covered by response
- **F1 Score** — harmonic mean of precision and recall
- **Reference** — the ground truth or expected answer

## Description

The metric evaluates factual correctness of the AI response compared to a reference answer. It decomposes both texts into atomic claims and uses Natural Language Inference (NLI) to verify which claims are supported, contradicted, or neutral.

Unlike Faithfulness (which compares response to context), Factual Correctness compares response directly to reference answer.

## Example

**Reference:** "Paris is the capital of France. The Eiffel Tower was completed in 1889."

**AI Response:** "Paris is the capital of France. The Eiffel Tower was built in 1500."

**Claim Analysis (Precision):**
1. "Paris is the capital of France" → SUPPORTED ✓
2. "The Eiffel Tower was built in 1500" → CONTRADICTED ✗

**Score (F1 mode):**
- Precision = 1/2 = 0.50
- Recall = 1/2 = 0.50 (assumes similar claims in reference)
- F1 = 2 × (0.50 × 0.50) / (0.50 + 0.50) = **0.50 (50%)**

---

**Good Response Example:**

**AI Response:** "The capital of France is Paris. The Eiffel Tower was completed in 1889."

**Score:** F1 = **1.00 (100%)** — all claims match reference

## Score Interpretation

|  Score  |                        Meaning                        |
|---------|-------------------------------------------------------|
| 90-100% | Excellent — all claims factually correct and complete |
| 70-90%  | Good — most claims correct with minor omissions       |
| 50-70%  | Moderate — some facts incorrect or missing            |
| 0-50%   | Poor — many factual errors or significant omissions   |

## Algorithm

1. **Decompose Response Claims** — LLM extracts atomic claims from AI response
2. **Decompose Reference Claims** — LLM extracts atomic claims from reference
3. **Verify Claims with NLI** — Each claim is verified using NLI:
   - Response claims against reference (for precision)
   - Reference claims against response (for recall)
4. **Compute Score** — Calculate precision, recall, and F1 score

## Formula

**F1 Mode (default):**

```
F1 = 2 × (precision × recall) / (precision + recall)
```

**Precision Mode:**

```
precision = supported_response_claims / total_response_claims
```

**Recall Mode:**

```
recall = supported_reference_claims / total_reference_claims
```

Where:
- `supported_claims` — claims with NLI verdict = SUPPORTED
- Claims with CONTRADICTED or NEUTRAL verdicts are not counted as supported

## NLI Verdicts

|   Verdict    |                     Meaning                      |
|--------------|--------------------------------------------------|
| SUPPORTED    | Claim can be directly inferred from the source   |
| CONTRADICTED | Claim is directly contradicted by the source     |
| NEUTRAL      | Claim cannot be verified (insufficient evidence) |

## Configuration

| Parameter | Type | Default |                 Description                  |
|-----------|------|---------|----------------------------------------------|
| mode      | Mode | F1      | Scoring mode: F1, PRECISION, or RECALL       |
| models    | List | all     | LLM models to use for multi-model evaluation |

## When to Use Each Mode

- **F1 (default)** — balanced evaluation of both accuracy and completeness
- **PRECISION** — focus on correctness (penalizes wrong claims, not missing info)
- **RECALL** — focus on completeness (penalizes missing info, not extra claims)

## Important Notes

- **NEUTRAL claims** are counted as NOT supported, which reduces the score
- **Extra correct information** in response doesn't hurt precision (if not contradicted)
- **Incomplete response** hurts recall but not precision
- **This metric requires LLM calls** for both claim decomposition and NLI verification

## References

- [RAGAS FactualCorrectness](https://github.com/explodinggradients/ragas)
- [Natural Language Inference](https://arxiv.org/abs/1508.05326)

