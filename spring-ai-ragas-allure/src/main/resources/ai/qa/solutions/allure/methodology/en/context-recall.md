# Context Recall Metric

## Glossary

- **Context** — information fragments retrieved by the search system
- **Reference (Ground Truth)** — the expected correct answer to the user's question
- **Attribution** — presence of reference information in the context
- **Agentic System** — an LLM-based system that generates responses

## Description

The metric evaluates the completeness of retrieved context relative to the reference answer. The reference is broken into individual sentences, each of which is checked for presence in the context. If information from the reference is missing from the context, the agentic system cannot use it in the response.

## Example

**Reference (Ground Truth):** "The Eiffel Tower was built in 1889. It is located in Paris, France. Gustave Eiffel designed it."

**Context:** "The Eiffel Tower was completed in 1889 for the World's Fair in Paris."

**Checking reference sentences:**
1. "Built in 1889" → found in context ✓
2. "Located in Paris, France" → found in context ✓
3. "Gustave Eiffel designed it" → not found in context ✗

**Score:** 2/3 = **0.67 (67%)**

## Score Interpretation

|  Score  |                               Meaning                               |
|---------|---------------------------------------------------------------------|
| 90-100% | Context contains all required information from the reference        |
| 70-90%  | Most information from the reference is present in context           |
| 50-70%  | Some key information is missing from context                        |
| 0-50%   | Context lacks significant portion of information from the reference |

## Algorithm

1. **Reference Splitting** — reference answer is broken into individual sentences
2. **Attribution Check** — LLM verifies presence of each sentence in context
3. **Score Calculation** — ratio of found sentences to total count

## Formula

```
score = attributed_sentences / total_sentences
```

Where:
- `attributed_sentences` — number of reference sentences found in context
- `total_sentences` — total number of sentences in the reference

## References

- [RAGAS Documentation](https://github.com/vibrantlabsai/ragas/blob/main/docs/concepts/metrics/available_metrics/context_recall.md)

